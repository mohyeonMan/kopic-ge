package io.jhpark.kopic.ge.command.app;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.game.domain.CanvasState;
import io.jhpark.kopic.ge.game.domain.DrawingPhase;
import io.jhpark.kopic.ge.game.domain.EndedPhase;
import io.jhpark.kopic.ge.game.domain.Game;
import io.jhpark.kopic.ge.game.domain.GameStatus;
import io.jhpark.kopic.ge.game.domain.Round;
import io.jhpark.kopic.ge.game.domain.RoundState;
import io.jhpark.kopic.ge.game.domain.ScoreBoard;
import io.jhpark.kopic.ge.game.domain.Stroke;
import io.jhpark.kopic.ge.game.domain.StrokeTool;
import io.jhpark.kopic.ge.game.domain.Turn;
import io.jhpark.kopic.ge.game.domain.TurnEndReason;
import io.jhpark.kopic.ge.game.domain.TurnPhase;
import io.jhpark.kopic.ge.game.domain.TurnState;
import io.jhpark.kopic.ge.game.domain.WordChoicePhase;
import io.jhpark.kopic.ge.outbound.app.BroadcastService;
import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import io.jhpark.kopic.ge.room.app.RoomFollowUp;
import io.jhpark.kopic.ge.room.app.RoomJob;
import io.jhpark.kopic.ge.room.app.RoomJobResult;
import io.jhpark.kopic.ge.room.app.RoomService;
import io.jhpark.kopic.ge.room.domain.DrawerOrderMode;
import io.jhpark.kopic.ge.room.domain.EndMode;
import io.jhpark.kopic.ge.room.domain.GameSettings;
import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.ParticipantStatus;
import io.jhpark.kopic.ge.room.domain.Room;
import io.jhpark.kopic.ge.room.domain.RoomState;
import io.jhpark.kopic.ge.room.domain.RoomType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class GameFlowService {

	private static final Duration PRIVATE_EMPTY_ROOM_CLOSE_DELAY = Duration.ofSeconds(30);
	private static final Duration RANDOM_EMPTY_ROOM_CLOSE_DELAY = Duration.ZERO;
	private static final int GUESS_RATE_LIMIT_PER_SEC = 5;
	private static final int DRAW_STROKE_RATE_LIMIT_PER_SEC = 30;
	private static final int TURN_END_RESULT_VIEW_SEC = 3;
	private static final Duration RATE_WINDOW_TTL = Duration.ofSeconds(120);
	private static final List<String> WORD_POOL = List.of(
		"apple", "banana", "cat", "train", "ocean",
		"camera", "bridge", "guitar", "rocket", "pencil"
	);

	private final RoomService roomService;
	private final BroadcastService broadcastService;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final Cache<String, RateWindow> rateWindows = Caffeine.newBuilder()
		.expireAfterAccess(RATE_WINDOW_TTL)
		.build();

	public GameFlowService(
		RoomService roomService,
		BroadcastService broadcastService
	) {
		this.roomService = roomService;
		this.broadcastService = broadcastService;
	}

	public EngineAck dispatch(RoomEventContext context) {
		return switch (context.eventType()) {
			case ROOM_JOIN -> handleJoin(context);
			case ROOM_LEAVE -> handleLeave(context);
			case GAME_SETTINGS_UPDATE_REQUEST -> handleGameSettingsUpdate(context);
			case GAME_SNAPSHOT_REQUEST -> handleGameSnapshotRequest(context);
			case GAME_START_REQUEST -> handleGameStart(context);
			case DRAW_STROKE -> handleDrawStroke(context);
			case DRAW_CLEAR -> handleDrawClear(context);
			case GUESS_SUBMIT -> handleGuessSubmit(context);
			case WORD_CHOICE -> handleWordChoice(context);
		};
	}

	private EngineAck handleJoin(RoomEventContext context) {
		String nickname = context.payload() == null ? null : context.payload().path("nickname").asText(null);
		if (nickname == null || nickname.isBlank()) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}
		try {
			roomService.submit(context.roomId(), joinJob(context.userId(), nickname));
			log.info("room join submitted. roomId={}, userId={}", context.roomId(), context.userId());
			return EngineAck.acceptedAck();
		} catch (IllegalStateException | IllegalArgumentException exception) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}
	}

	private EngineAck handleLeave(RoomEventContext context) {
		roomService.submit(context.roomId(), leaveJob(context.userId()));
		log.info("room leave submitted. roomId={}, userId={}", context.roomId(), context.userId());
		return EngineAck.acceptedAck();
	}

	private EngineAck handleGameSettingsUpdate(RoomEventContext context) {
		try {
			GameSettings settings = parseSettings(context.payload());
			roomService.submit(context.roomId(), gameSettingsUpdateJob(context.userId(), settings));
			log.info("game settings update submitted. roomId={}, userId={}", context.roomId(), context.userId());
			return EngineAck.acceptedAck();
		} catch (IllegalArgumentException exception) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}
	}

	private EngineAck handleGameSnapshotRequest(RoomEventContext context) {
		boolean ignore = roomService.findRoom(context.roomId())
			.map(room -> !room.getParticipants().containsKey(context.userId()))
			.orElse(true);
		if (ignore) {
			log.info("game snapshot ignored. roomId={}, userId={}", context.roomId(), context.userId());
			return EngineAck.acceptedAck();
		}
		roomService.submit(context.roomId(), snapshotJob(context.userId(), context.requestId()));
		log.info("game snapshot submitted. roomId={}, userId={}", context.roomId(), context.userId());
		return EngineAck.acceptedAck();
	}

	private EngineAck handleGameStart(RoomEventContext context) {
		roomService.submit(context.roomId(), gameStartJob(context.userId()));
		log.info("game start submitted. roomId={}, userId={}", context.roomId(), context.userId());
		return EngineAck.acceptedAck();
	}

	private EngineAck handleDrawStroke(RoomEventContext context) {
		if (!allowPerSecond("draw:" + context.userId(), DRAW_STROKE_RATE_LIMIT_PER_SEC)) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}
		try {
			String turnId = context.payload().path("turnId").asText(null);
			Stroke stroke = parseStroke(context.payload().path("stroke"));
			roomService.submit(context.roomId(), drawStrokeJob(context.userId(), turnId, stroke));
			log.info("draw stroke submitted. roomId={}, userId={}, turnId={}", context.roomId(), context.userId(), turnId);
			return EngineAck.acceptedAck();
		} catch (IllegalArgumentException exception) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}
	}

	private EngineAck handleDrawClear(RoomEventContext context) {
		String turnId = context.payload().path("turnId").asText(null);
		if (turnId == null || turnId.isBlank()) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}
		roomService.submit(context.roomId(), drawClearJob(context.userId(), turnId));
		log.info("draw clear submitted. roomId={}, userId={}, turnId={}", context.roomId(), context.userId(), turnId);
		return EngineAck.acceptedAck();
	}

	private EngineAck handleGuessSubmit(RoomEventContext context) {
		if (!allowPerSecond("guess:" + context.userId(), GUESS_RATE_LIMIT_PER_SEC)) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}
		String text = context.payload().path("text").asText(null);
		String turnId = context.payload().path("turnId").asText(null);
		if (text == null || text.isBlank()) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}
		roomService.submit(context.roomId(), guessSubmitJob(context.userId(), turnId, text));
		log.info("guess submit submitted. roomId={}, userId={}, turnId={}", context.roomId(), context.userId(), turnId);
		return EngineAck.acceptedAck();
	}

	private EngineAck handleWordChoice(RoomEventContext context) {
		if (context.payload() == null || !context.payload().has("choiceIndex")) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}

		int choiceIndex = context.payload().path("choiceIndex").asInt(-1);
		if (choiceIndex < 0) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		}

		roomService.submit(context.roomId(), explicitWordChoiceJob(context.userId(), choiceIndex));
		log.info("word choice submitted. roomId={}, userId={}, choiceIndex={}",
			context.roomId(), context.userId(), choiceIndex);
		return EngineAck.acceptedAck();
	}

	private RoomJob joinJob(String userId, String nickname) {
		return room -> {
			if (room.getParticipants().containsKey(userId)) {
				return RoomJobResult.keep();
			}
			if (!room.getParticipants().containsKey(userId) && room.getParticipants().size() >= room.getCapacity()) {
				throw new IllegalStateException("room is full");
			}
			room.getParticipants().put(
				userId,
				new Participant(userId, nickname == null || nickname.isBlank() ? userId : nickname, ParticipantStatus.ACTIVE)
			);
			if (room.getCurrentGame() != null) {
				room.getCurrentGame().scores().scores().putIfAbsent(userId, 0);
			}
			room.increaseVersion();
			broadcastService.toUsers(
				otherUserIds(room.getParticipants().keySet(), userId),
				envelope(OutboundRoomEventType.ROOM_JOINED, roomJoinedPayload(room, userId), null)
			);
			broadcastService.toUser(
				userId,
				envelope(OutboundRoomEventType.GAME_SNAPSHOT, snapshotPayload(room), null)
			);
			return RoomJobResult.keep();
		};
	}

	private RoomJob leaveJob(String userId) {
		return room -> {
			Participant removed = room.getParticipants().remove(userId);
			if (removed == null) {
				return RoomJobResult.keep();
			}
			if (room.getRoomType() == RoomType.PRIVATE && userId.equals(room.getHostUserId())) {
				room.setHostUserId(room.getParticipants().keySet().stream().findFirst().orElse(null));
			}
			if (room.getParticipants().isEmpty()) {
				room.increaseVersion();
				Duration delay = room.getRoomType() == RoomType.PRIVATE
					? PRIVATE_EMPTY_ROOM_CLOSE_DELAY
					: RANDOM_EMPTY_ROOM_CLOSE_DELAY;
				return RoomJobResult.keepWith(RoomFollowUp.delayed(delay, closeEmptyRoomJob()));
			}
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.ROOM_LEFT, roomLeftPayload(room, userId), null)
			);
			if (room.getCurrentGame() != null
				&& room.getCurrentGame().currentRound() != null
				&& room.getCurrentGame().currentRound().getCurrentTurn() != null
				&& userId.equals(room.getCurrentGame().currentRound().getCurrentTurn().getDrawerUserId())
				&& room.getCurrentGame().currentRound().getCurrentTurn().getState() != TurnState.ENDED) {
				return turnEndJob(room.getCurrentGame().currentRound().getCurrentTurn().getTurnId(), TurnEndReason.DRAWER_LEFT).run(room);
			}
			return RoomJobResult.keep();
		};
	}

	private RoomJob gameSettingsUpdateJob(String requestedUserId, GameSettings settings) {
		return room -> {
			if (room.getRoomType() != RoomType.PRIVATE) {
				return RoomJobResult.keep();
			}
			if (!requestedUserId.equals(room.getHostUserId())) {
				return RoomJobResult.keep();
			}
			if (room.getState() != RoomState.LOBBY || room.getCurrentGame() != null) {
				return RoomJobResult.keep();
			}
			GameSettings previousSettings = room.getSettings();
			if (settings.equals(previousSettings)) {
				return RoomJobResult.keep();
			}
			room.setSettings(settings);
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.GAME_SETTINGS_UPDATED, gameSettingsUpdatedPayload(previousSettings, settings), null)
			);
			return RoomJobResult.keep();
		};
	}

	private RoomJob gameStartJob(String requestedUserId) {
		return room -> {
			if (room.getRoomType() != RoomType.PRIVATE) {
				return RoomJobResult.keep();
			}
			if (room.getHostUserId() == null || !room.getHostUserId().equals(requestedUserId)) {
				return RoomJobResult.keep();
			}
			if (room.getCurrentGame() != null || room.getState() != RoomState.LOBBY) {
				return RoomJobResult.keep();
			}
			if (room.getParticipants().size() < 2) {
				return RoomJobResult.keep();
			}
			if (room.getSettings() == null) {
				return RoomJobResult.keep();
			}

			Instant now = Instant.now();
			Game game = new Game(
				nextGameId(),
				room.getRoomId(),
				GameStatus.RUNNING,
				room.getSettings(),
				new ScoreBoard(initialScores(room)),
				null,
				now,
				null,
				null
			);

			room.setCurrentGame(game);
			room.setState(RoomState.RUNNING);
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.GAME_STARTED, gameStartedPayload(room), null)
			);

			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(Duration.ofSeconds(2), startRoundJob(1))
			);
		};
	}

	private RoomJob startRoundJob(int roundNo) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.status() != GameStatus.RUNNING) {
				return RoomJobResult.keep();
			}

			Instant now = Instant.now();
			Round round = new Round(
				roundNo,
				RoundState.RUNNING,
				0,
				createDrawerOrder(room, game.settings().drawerOrderMode()),
				null,
				now,
				null
			);
			game.currentRound(round);
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.ROUND_STARTED, roundStartedPayload(room), null)
			);

			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(Duration.ofSeconds(2), startWordChoiceTurnJob(roundNo, 0))
			);
		};
	}

	private RoomJob startWordChoiceTurnJob(int roundNo, int turnCursor) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null) {
				return RoomJobResult.keep();
			}

			Round round = game.currentRound();
			if (round.getRoundNo() != roundNo || round.getState() != RoundState.RUNNING) {
				return RoomJobResult.keep();
			}

			String drawerUserId = chooseDrawerUserId(round, turnCursor);
			if (drawerUserId == null) {
				return RoomJobResult.keep();
			}

			Instant now = Instant.now();
			Turn turn = new Turn(
				nextTurnId(),
				drawerUserId,
				TurnState.WORD_CHOICE,
				new LinkedHashSet<>(),
				new LinkedHashMap<>(),
				null,
				null
			);

			round.setTurnCursor(turnCursor);
			round.setCurrentTurn(turn);
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.TURN_STARTED, turnStartedPayload(room), null)
			);

			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(
					Duration.ofSeconds(2),
					openWordChoiceWindowJob(turn.getTurnId())
				)
			);
		};
	}

	private RoomJob openWordChoiceWindowJob(String expectedTurnId) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
				return RoomJobResult.keep();
			}

			Turn turn = game.currentRound().getCurrentTurn();
			if (!turn.getTurnId().equals(expectedTurnId) || turn.getState() != TurnState.WORD_CHOICE) {
				return RoomJobResult.keep();
			}
			Instant now = Instant.now();
			turn.setPhase(new WordChoicePhase(
				createWordChoices(room.getRoomId(), game.settings().wordChoiceCount()),
				now,
				now.plusSeconds(game.settings().wordChoiceSec())
			));

			broadcastService.toUser(
				turn.getDrawerUserId(),
				envelope(OutboundRoomEventType.WORD_CHOICES, wordChoicesPayload(room), null)
			);
			broadcastService.toUsers(
				otherUserIds(room.getParticipants().keySet(), turn.getDrawerUserId()),
				envelope(OutboundRoomEventType.TURN_STATE, turnStatePayload(room, "WORD_CHOICES_GIVEN"), null)
			);
			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(
					Duration.ofSeconds(game.settings().wordChoiceSec()),
					wordChoiceTimeoutJob(turn.getTurnId())
				)
			);
		};
	}

	private RoomJob explicitWordChoiceJob(String userId, int choiceIndex) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
				return RoomJobResult.keep();
			}

			Turn turn = game.currentRound().getCurrentTurn();
			if (turn.getState() != TurnState.WORD_CHOICE) {
				return RoomJobResult.keep();
			}
			if (!turn.getDrawerUserId().equals(userId)) {
				return RoomJobResult.keep();
			}

			WordChoicePhase phase = requireWordChoicePhase(turn.getPhase());
			if (phase.getEndsAt() == null || !Instant.now().isBefore(phase.getEndsAt())) {
				return wordChoiceTimeoutJob(turn.getTurnId()).run(room);
			}
			List<String> choices = phase.getWordChoices();
			if (choiceIndex < 0 || choiceIndex >= choices.size()) {
				return RoomJobResult.keep();
			}

			applyDrawingPhase(room, choices.get(choiceIndex), Instant.now());
			broadcastService.toUser(
				turn.getDrawerUserId(),
				envelope(OutboundRoomEventType.DRAWING_STARTED, drawingStartedPayload(room), null)
			);
			broadcastService.toUsers(
				otherUserIds(room.getParticipants().keySet(), turn.getDrawerUserId()),
				envelope(OutboundRoomEventType.TURN_STATE, turnStatePayload(room, "DRAWING_STARTED"), null)
			);
			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(
					Duration.ofSeconds(game.settings().drawSec()),
					drawingTimeoutJob(turn.getTurnId())
				)
			);
		};
	}

	private RoomJob wordChoiceTimeoutJob(String expectedTurnId) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
				return RoomJobResult.keep();
			}

			Turn turn = game.currentRound().getCurrentTurn();
			if (!turn.getTurnId().equals(expectedTurnId) || turn.getState() != TurnState.WORD_CHOICE) {
				return RoomJobResult.keep();
			}

			WordChoicePhase phase = requireWordChoicePhase(turn.getPhase());
			List<String> choices = phase.getWordChoices();
			if (choices.isEmpty()) {
				return RoomJobResult.keep();
			}

			int choiceIndex = ThreadLocalRandom.current().nextInt(choices.size());
			applyDrawingPhase(room, choices.get(choiceIndex), Instant.now());
			broadcastService.toUser(
				turn.getDrawerUserId(),
				envelope(OutboundRoomEventType.DRAWING_STARTED, drawingStartedPayload(room), null)
			);
			broadcastService.toUsers(
				otherUserIds(room.getParticipants().keySet(), turn.getDrawerUserId()),
				envelope(OutboundRoomEventType.TURN_STATE, turnStatePayload(room, "DRAWING_STARTED"), null)
			);
			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(
					Duration.ofSeconds(game.settings().drawSec()),
					drawingTimeoutJob(turn.getTurnId())
				)
			);
		};
	}

	private RoomJob drawingTimeoutJob(String expectedTurnId) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
				return RoomJobResult.keep();
			}

			Turn turn = game.currentRound().getCurrentTurn();
			if (!turn.getTurnId().equals(expectedTurnId) || turn.getState() != TurnState.DRAWING) {
				return RoomJobResult.keep();
			}

			return turnEndJob(expectedTurnId, TurnEndReason.TIMEOUT).run(room);
		};
	}

	private RoomJob turnEndJob(String expectedTurnId, TurnEndReason endReason) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
				return RoomJobResult.keep();
			}

			Turn turn = game.currentRound().getCurrentTurn();
			if (!turn.getTurnId().equals(expectedTurnId) || turn.getState() == TurnState.ENDED) {
				return RoomJobResult.keep();
			}

			commitPendingScores(room, turn);
			applyTurnEnd(room, endReason, Instant.now());
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.TURN_ENDED, turnEndedPayload(room), null)
			);

			Round round = game.currentRound();
			if (round.getTurnCursor() + 1 < round.getDrawerOrder().size()) {
				return RoomJobResult.keepWith(
					RoomFollowUp.delayed(Duration.ofSeconds(TURN_END_RESULT_VIEW_SEC), startWordChoiceTurnJob(round.getRoundNo(), round.getTurnCursor() + 1))
				);
			}
			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(Duration.ofSeconds(TURN_END_RESULT_VIEW_SEC), roundEndJob(round.getRoundNo()))
			);
		};
	}

	private RoomJob roundEndJob(int expectedRoundNo) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null) {
				return RoomJobResult.keep();
			}
			Round round = game.currentRound();
			if (round.getRoundNo() != expectedRoundNo || round.getState() == RoundState.ENDED) {
				return RoomJobResult.keep();
			}

			round.setState(RoundState.ENDED);
			round.setEndedAt(Instant.now());
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.ROUND_ENDED, roundEndedPayload(room), null)
			);

			if (expectedRoundNo < game.settings().roundCount()) {
				return RoomJobResult.keepWith(
					RoomFollowUp.immediate(startRoundJob(expectedRoundNo + 1))
				);
			}
			return RoomJobResult.keepWith(RoomFollowUp.immediate(gameEndJob()));
		};
	}

	private RoomJob gameEndJob() {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.status() == GameStatus.RESULT_VIEW || game.status() == GameStatus.ENDED) {
				return RoomJobResult.keep();
			}

			Instant now = Instant.now();
			game.status(GameStatus.RESULT_VIEW);
			game.endedAt(now);
			game.resultViewUntil(now.plusSeconds(8));
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.GAME_ENDED, gameEndedPayload(room), null)
			);
			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(Duration.ofSeconds(8), resultViewEndJob())
			);
		};
	}

	private RoomJob resultViewEndJob() {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.status() != GameStatus.RESULT_VIEW) {
				return RoomJobResult.keep();
			}
			if (room.getRoomType() == RoomType.PRIVATE) {
				game.status(GameStatus.ENDED);
				room.setCurrentGame(null);
				room.setState(RoomState.LOBBY);
				room.increaseVersion();
				return RoomJobResult.keep();
			}
			if (room.getParticipants().size() < 2) {
				game.status(GameStatus.ENDED);
				room.setCurrentGame(null);
				room.setState(RoomState.LOBBY);
				room.increaseVersion();
				return RoomJobResult.keep();
			}

			Game nextGame = new Game(
				nextGameId(),
				room.getRoomId(),
				GameStatus.RUNNING,
				room.getSettings(),
				new ScoreBoard(initialScores(room)),
				null,
				Instant.now(),
				null,
				null
			);
			room.setCurrentGame(nextGame);
			room.setState(RoomState.RUNNING);
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.GAME_STARTED, gameStartedPayload(room), null)
			);
			return RoomJobResult.keepWith(RoomFollowUp.delayed(Duration.ofSeconds(2), startRoundJob(1)));
		};
	}

	private RoomJob drawStrokeJob(String userId, String expectedTurnId, Stroke stroke) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
				return RoomJobResult.keep();
			}
			Turn turn = game.currentRound().getCurrentTurn();
			if (turn.getState() != TurnState.DRAWING || !turn.getDrawerUserId().equals(userId)) {
				return RoomJobResult.keep();
			}
			if (expectedTurnId == null || !turn.getTurnId().equals(expectedTurnId)) {
				return RoomJobResult.keep();
			}
			DrawingPhase phase = requireDrawingPhase(turn.getPhase());
			phase.getCanvas().getStrokes().add(stroke);
			room.increaseVersion();
			broadcastService.toUsers(
				otherUserIds(room.getParticipants().keySet(), userId),
				envelope(OutboundRoomEventType.CANVAS_STROKE, canvasStrokePayload(turn, stroke), null)
			);
			return RoomJobResult.keep();
		};
	}

	private RoomJob drawClearJob(String userId, String expectedTurnId) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
				return RoomJobResult.keep();
			}
			Turn turn = game.currentRound().getCurrentTurn();
			if (turn.getState() != TurnState.DRAWING || !turn.getDrawerUserId().equals(userId)) {
				return RoomJobResult.keep();
			}
			if (!turn.getTurnId().equals(expectedTurnId)) {
				return RoomJobResult.keep();
			}
			DrawingPhase phase = requireDrawingPhase(turn.getPhase());
			phase.getCanvas().getStrokes().clear();
			room.increaseVersion();
			broadcastService.toUsers(
				otherUserIds(room.getParticipants().keySet(), userId),
				envelope(OutboundRoomEventType.CANVAS_CLEAR, canvasClearPayload(turn), null)
			);
			return RoomJobResult.keep();
		};
	}

	private RoomJob guessSubmitJob(String userId, String expectedTurnId, String text) {
		return room -> {
			Game game = room.getCurrentGame();
			if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
				return RoomJobResult.keep();
			}
			Turn turn = game.currentRound().getCurrentTurn();
			if (turn.getState() != TurnState.DRAWING) {
				return RoomJobResult.keep();
			}
			if (expectedTurnId != null && !expectedTurnId.isBlank() && !turn.getTurnId().equals(expectedTurnId)) {
				return RoomJobResult.keep();
			}
			Participant participant = room.getParticipants().get(userId);
			String nickname = participant == null ? userId : participant.nickname();
			boolean drawer = turn.getDrawerUserId().equals(userId);

			if (drawer) {
				broadcastService.toUsers(
					correctUsersAndDrawer(turn),
					envelope(OutboundRoomEventType.GUESS_MESSAGE, guessMessagePayload(userId, nickname, text, turn.getTurnId()), null)
				);
				return RoomJobResult.keep();
			}

			DrawingPhase drawingPhase = requireDrawingPhase(turn.getPhase());
			String normalizedText = normalizeGuess(text);
			String normalizedAnswer = normalizeGuess(drawingPhase.getSecretWord());

			if (!normalizedText.equals(normalizedAnswer)) {
				ServerEnvelope envelope = envelope(OutboundRoomEventType.GUESS_MESSAGE, guessMessagePayload(userId, nickname, text, turn.getTurnId()), null);
				if (turn.getCorrectUserIds().contains(userId)) {
					broadcastService.toUsers(correctUsersAndDrawer(turn), envelope);
				} else {
					broadcastService.toUsers(room.getParticipants().keySet(), envelope);
				}
				return RoomJobResult.keep();
			}

			if (turn.getCorrectUserIds().contains(userId)) {
				broadcastService.toUsers(
					correctUsersAndDrawer(turn),
					envelope(OutboundRoomEventType.GUESS_MESSAGE, guessMessagePayload(userId, nickname, text, turn.getTurnId()), null)
				);
				return RoomJobResult.keep();
			}

			turn.getCorrectUserIds().add(userId);
			turn.getPendingScores().merge(userId, 1, Integer::sum);
			turn.getPendingScores().merge(turn.getDrawerUserId(), 1, Integer::sum);
			room.increaseVersion();
			broadcastService.toUsers(
				room.getParticipants().keySet(),
				envelope(OutboundRoomEventType.GUESS_CORRECT, guessCorrectPayload(userId, nickname, turn), null)
			);

			boolean shouldEnd = game.settings().endMode() == EndMode.FIRST_CORRECT
				|| turn.getCorrectUserIds().size() >= Math.max(0, room.getParticipants().size() - 1);
			if (shouldEnd) {
				TurnEndReason reason = game.settings().endMode() == EndMode.FIRST_CORRECT
					? TurnEndReason.FIRST_CORRECT
					: TurnEndReason.ALL_CORRECT;
				return turnEndJob(turn.getTurnId(), reason).run(room);
			}
			return RoomJobResult.keep();
		};
	}

	private RoomJob snapshotJob(String userId, String requestId) {
		return room -> {
			if (!room.getParticipants().containsKey(userId)) {
				return RoomJobResult.keep();
			}
			broadcastService.toUser(
				userId,
				envelope(OutboundRoomEventType.GAME_SNAPSHOT, snapshotPayload(room), requestId)
			);
			return RoomJobResult.keep();
		};
	}

	private RoomJob closeEmptyRoomJob() {
		return room -> room.getParticipants().isEmpty()
			? RoomJobResult.deleteSlot()
			: RoomJobResult.keep();
	}

	private Map<String, Integer> initialScores(Room room) {
		Map<String, Integer> scores = new LinkedHashMap<>();
		for (String userId : room.getParticipants().keySet()) {
			scores.put(userId, 0);
		}
		return scores;
	}

	private String nextGameId() {
		return "g-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private String nextTurnId() {
		return "t-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private List<String> createDrawerOrder(Room room, DrawerOrderMode drawerOrderMode) {
		List<String> drawerOrder = new ArrayList<>(room.getParticipants().keySet());
		if (drawerOrderMode == DrawerOrderMode.RANDOM) {
			Collections.shuffle(drawerOrder, ThreadLocalRandom.current());
		}
		return drawerOrder;
	}

	private String chooseDrawerUserId(Round round, int turnCursor) {
		if (round.getDrawerOrder().isEmpty()) {
			return null;
		}
		return round.getDrawerOrder().get(Math.floorMod(turnCursor, round.getDrawerOrder().size()));
	}

	private List<String> createWordChoices(String roomId, int count) {
		int start = Math.floorMod(roomId.hashCode() + ThreadLocalRandom.current().nextInt(), WORD_POOL.size());
		List<String> choices = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			choices.add(WORD_POOL.get((start + i) % WORD_POOL.size()));
		}
		return choices;
	}

	private WordChoicePhase requireWordChoicePhase(TurnPhase phase) {
		if (phase instanceof WordChoicePhase wordChoicePhase) {
			return wordChoicePhase;
		}
		throw new IllegalStateException("turn phase must be WordChoicePhase");
	}

	private DrawingPhase requireDrawingPhase(TurnPhase phase) {
		if (phase instanceof DrawingPhase drawingPhase) {
			return drawingPhase;
		}
		throw new IllegalStateException("turn phase must be DrawingPhase");
	}

	private void applyDrawingPhase(Room room, String secretWord, Instant now) {
		Game game = room.getCurrentGame();
		Turn turn = game.currentRound().getCurrentTurn();

		turn.setState(TurnState.DRAWING);
		turn.setEndReason(null);
		turn.setPhase(new DrawingPhase(
			secretWord,
			new CanvasState(new ArrayList<>()),
			now,
			now.plusSeconds(game.settings().drawSec())
		));
		room.increaseVersion();
	}

	private void applyTurnEnd(Room room, TurnEndReason reason, Instant now) {
		Game game = room.getCurrentGame();
		Turn turn = game.currentRound().getCurrentTurn();

		turn.setState(TurnState.ENDED);
		turn.setEndReason(reason);
		turn.setPhase(new EndedPhase(now));
		room.increaseVersion();
	}

	private ServerEnvelope envelope(OutboundRoomEventType eventType, ObjectNode payload, String requestId) {
		return new ServerEnvelope(eventType.eventCode(), payload, requestId);
	}

	private ObjectNode roomJoinedPayload(Room room, String joinedUserId) {
		Participant participant = room.getParticipants().get(joinedUserId);
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("userId", joinedUserId);
		payload.put("nickname", participant == null ? joinedUserId : participant.nickname());
		return payload;
	}

	private ObjectNode roomLeftPayload(Room room, String leftUserId) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("userId", leftUserId);
		return payload;
	}

	private ObjectNode gameSettingsUpdatedPayload(GameSettings previousSettings, GameSettings settings) {
		ObjectNode payload = objectMapper.createObjectNode();
		ObjectNode settingsNode = objectMapper.createObjectNode();
		if (previousSettings == null || previousSettings.roundCount() != settings.roundCount()) {
			settingsNode.put("roundCount", settings.roundCount());
		}
		if (previousSettings == null || previousSettings.drawSec() != settings.drawSec()) {
			settingsNode.put("drawSec", settings.drawSec());
		}
		if (previousSettings == null || previousSettings.wordChoiceSec() != settings.wordChoiceSec()) {
			settingsNode.put("wordChoiceSec", settings.wordChoiceSec());
		}
		if (previousSettings == null || previousSettings.wordChoiceCount() != settings.wordChoiceCount()) {
			settingsNode.put("wordChoiceCount", settings.wordChoiceCount());
		}
		if (previousSettings == null || previousSettings.drawerOrderMode() != settings.drawerOrderMode()) {
			settingsNode.put("drawerOrderMode", settings.drawerOrderMode().name());
		}
		if (previousSettings == null || previousSettings.endMode() != settings.endMode()) {
			settingsNode.put("endMode", settings.endMode().name());
		}
		payload.set("settings", settingsNode);
		return payload;
	}

	private ObjectNode gameStartedPayload(Room room) {
		Game game = room.getCurrentGame();
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("gameId", game.gameId());
		ObjectNode settings = objectMapper.createObjectNode();
		settings.put("roundCount", game.settings().roundCount());
		settings.put("drawSec", game.settings().drawSec());
		settings.put("wordChoiceSec", game.settings().wordChoiceSec());
		settings.put("wordChoiceCount", game.settings().wordChoiceCount());
		settings.put("drawerOrderMode", game.settings().drawerOrderMode().name());
		settings.put("endMode", game.settings().endMode().name());
		payload.set("settings", settings);
		return payload;
	}

	private ObjectNode roundStartedPayload(Room room) {
		Game game = room.getCurrentGame();
		Round round = game.currentRound();
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("gameId", game.gameId());
		payload.put("round", round.getRoundNo());
		return payload;
	}

	private ObjectNode turnStartedPayload(Room room) {
		Game game = room.getCurrentGame();
		Round round = game.currentRound();
		Turn turn = round.getCurrentTurn();
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("gameId", game.gameId());
		payload.put("round", round.getRoundNo());
		payload.put("turn", round.getTurnCursor() + 1);
		payload.put("turnId", turn.getTurnId());
		payload.put("drawerUserId", turn.getDrawerUserId());
		return payload;
	}

	private ObjectNode wordChoicesPayload(Room room) {
		Game game = room.getCurrentGame();
		Round round = game.currentRound();
		Turn turn = round.getCurrentTurn();
		ObjectNode payload = objectMapper.createObjectNode();
		ArrayNode choices = objectMapper.createArrayNode();
		requireWordChoicePhase(turn.getPhase()).getWordChoices().forEach(choices::add);
		payload.set("choices", choices);
		payload.put("timeoutSec", game.settings().wordChoiceSec());
		return payload;
	}

	private ObjectNode drawingStartedPayload(Room room) {
		Game game = room.getCurrentGame();
		Round round = game.currentRound();
		Turn turn = round.getCurrentTurn();
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("gameId", game.gameId());
		payload.put("round", round.getRoundNo());
		payload.put("turn", round.getTurnCursor() + 1);
		payload.put("turnId", turn.getTurnId());
		payload.put("drawerUserId", turn.getDrawerUserId());
		payload.put("durationSec", game.settings().drawSec());
		return payload;
	}

	private ObjectNode turnStatePayload(Room room, String phase) {
		Game game = room.getCurrentGame();
		Round round = game.currentRound();
		Turn turn = round.getCurrentTurn();
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("gameId", game.gameId());
		payload.put("round", round.getRoundNo());
		payload.put("turn", round.getTurnCursor() + 1);
		payload.put("turnId", turn.getTurnId());
		payload.put("phase", phase);
		payload.put("drawerUserId", turn.getDrawerUserId());
		if ("WORD_CHOICES_GIVEN".equals(phase)) {
			payload.put("timeoutSec", game.settings().wordChoiceSec());
		}
		if ("DRAWING_STARTED".equals(phase)) {
			payload.put("durationSec", game.settings().drawSec());
		}
		return payload;
	}

	private List<String> otherUserIds(Iterable<String> userIds, String excludedUserId) {
		List<String> result = new ArrayList<>();
		for (String userId : userIds) {
			if (!userId.equals(excludedUserId)) {
				result.add(userId);
			}
		}
		return result;
	}

	private List<String> correctUsersAndDrawer(Turn turn) {
		List<String> userIds = new ArrayList<>(turn.getCorrectUserIds());
		if (!userIds.contains(turn.getDrawerUserId())) {
			userIds.add(turn.getDrawerUserId());
		}
		return userIds;
	}

	private ObjectNode canvasStrokePayload(Turn turn, Stroke stroke) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("turnId", turn.getTurnId());
		payload.put("drawerUserId", turn.getDrawerUserId());
		payload.set("stroke", strokeNode(stroke));
		return payload;
	}

	private ObjectNode canvasClearPayload(Turn turn) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("turnId", turn.getTurnId());
		payload.put("drawerUserId", turn.getDrawerUserId());
		return payload;
	}

	private ObjectNode guessMessagePayload(String userId, String nickname, String text, String turnId) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("userId", userId);
		payload.put("nickname", nickname);
		payload.put("text", text);
		payload.put("turnId", turnId);
		return payload;
	}

	private ObjectNode guessCorrectPayload(String userId, String nickname, Turn turn) {
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("userId", userId);
		payload.put("nickname", nickname);
		payload.put("turnId", turn.getTurnId());
		return payload;
	}

	private ObjectNode turnEndedPayload(Room room) {
		Game game = room.getCurrentGame();
		Round round = game.currentRound();
		Turn turn = round.getCurrentTurn();
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("gameId", game.gameId());
		payload.put("round", round.getRoundNo());
		payload.put("turn", round.getTurnCursor() + 1);
		payload.put("turnId", turn.getTurnId());
		payload.put("reason", turn.getEndReason().name());
		payload.set("earnedScores", scoreEntries(turn.getPendingScores()));
		payload.set("scores", scoreEntries(game.scores().scores()));
		return payload;
	}

	private ObjectNode roundEndedPayload(Room room) {
		Game game = room.getCurrentGame();
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("gameId", game.gameId());
		payload.put("round", game.currentRound().getRoundNo());
		return payload;
	}

	private ObjectNode gameEndedPayload(Room room) {
		Game game = room.getCurrentGame();
		ObjectNode payload = objectMapper.createObjectNode();
		payload.put("gameId", game.gameId());
		payload.set("ranking", scoreEntries(sortedScores(game.scores().scores())));
		payload.put("resultViewSec", 8);
		return payload;
	}

	private ObjectNode snapshotPayload(Room room) {
		ObjectNode payload = objectMapper.createObjectNode();
		ObjectNode roomNode = objectMapper.createObjectNode();
		roomNode.put("roomId", room.getRoomId());
		roomNode.put("roomType", room.getRoomType().name().toLowerCase());
		if (room.getRoomCode() != null) {
			roomNode.put("roomCode", room.getRoomCode());
		}
		if (room.getHostUserId() != null) {
			roomNode.put("hostUserId", room.getHostUserId());
		}
		payload.set("room", roomNode);
		ArrayNode participantsNode = objectMapper.createArrayNode();
		for (Participant participant : room.getParticipants().values()) {
			ObjectNode participantNode = objectMapper.createObjectNode();
			participantNode.put("userId", participant.userId());
			participantNode.put("nickname", participant.nickname());
			participantsNode.add(participantNode);
		}
		payload.set("participants", participantsNode);

		ObjectNode gameNode = objectMapper.createObjectNode();
		Game game = room.getCurrentGame();
		if (game == null) {
			gameNode.put("status", "LOBBY");
			payload.set("game", gameNode);
			payload.set("canvas", emptyCanvasPayload());
			payload.set("scores", objectMapper.createArrayNode());
			return payload;
		}

		gameNode.put("status", game.status().name());
		gameNode.put("gameId", game.gameId());
		gameNode.set("settings", gameStartedPayload(room).with("settings"));
		if (game.currentRound() != null) {
			gameNode.put("round", game.currentRound().getRoundNo());
			if (game.currentRound().getCurrentTurn() != null) {
				Turn turn = game.currentRound().getCurrentTurn();
				gameNode.put("turn", game.currentRound().getTurnCursor() + 1);
				gameNode.put("turnId", turn.getTurnId());
				gameNode.put("drawerUserId", turn.getDrawerUserId());
				gameNode.put("remainingSec", remainingSec(turn, game));
			}
		}
		payload.set("game", gameNode);
		payload.set("canvas", canvasPayload(game));
		payload.set("scores", scoreEntries(game.scores().scores()));
		return payload;
	}

	private ObjectNode emptyCanvasPayload() {
		ObjectNode canvas = objectMapper.createObjectNode();
		canvas.set("strokes", objectMapper.createArrayNode());
		return canvas;
	}

	private ObjectNode canvasPayload(Game game) {
		ObjectNode canvas = objectMapper.createObjectNode();
		ArrayNode strokes = objectMapper.createArrayNode();
		if (game.currentRound() != null && game.currentRound().getCurrentTurn() != null) {
			TurnPhase phase = game.currentRound().getCurrentTurn().getPhase();
			if (phase instanceof DrawingPhase drawingPhase) {
				drawingPhase.getCanvas().getStrokes().forEach(stroke -> strokes.add(strokeNode(stroke)));
			}
		}
		canvas.set("strokes", strokes);
		return canvas;
	}

	private int remainingSec(Turn turn, Game game) {
		Instant now = Instant.now();
		if (turn.getPhase() instanceof WordChoicePhase wordChoicePhase) {
			if (wordChoicePhase.getEndsAt() == null) {
				return 0;
			}
			return Math.max(0, (int) Duration.between(now, wordChoicePhase.getEndsAt()).toSeconds());
		}
		if (turn.getPhase() instanceof DrawingPhase drawingPhase) {
			return Math.max(0, (int) Duration.between(now, drawingPhase.getEndsAt()).toSeconds());
		}
		if (game.resultViewUntil() != null) {
			return Math.max(0, (int) Duration.between(now, game.resultViewUntil()).toSeconds());
		}
		return 0;
	}

	private ArrayNode scoreEntries(Map<String, Integer> scores) {
		ArrayNode array = objectMapper.createArrayNode();
		for (Map.Entry<String, Integer> entry : scores.entrySet()) {
			ObjectNode node = objectMapper.createObjectNode();
			node.put("userId", entry.getKey());
			node.put("score", entry.getValue());
			array.add(node);
		}
		return array;
	}

	private Map<String, Integer> sortedScores(Map<String, Integer> scores) {
		return scores.entrySet().stream()
			.sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
			.collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
	}

	private com.fasterxml.jackson.databind.JsonNode strokeNode(Stroke stroke) {
		ArrayNode strokeNode = objectMapper.createArrayNode();
		strokeNode.add(stroke.strokeId());
		strokeNode.add(stroke.tool() == StrokeTool.PEN ? 1 : 2);
		strokeNode.add(stroke.colorIndex());
		strokeNode.add(stroke.size());
		ArrayNode points = objectMapper.createArrayNode();
		for (Stroke.Point point : stroke.points()) {
			ArrayNode pointNode = objectMapper.createArrayNode();
			pointNode.add(point.x());
			pointNode.add(point.y());
			points.add(pointNode);
		}
		strokeNode.add(points);
		return strokeNode;
	}

	private Stroke parseStroke(JsonNode node) {
		if (node == null || !node.isArray() || node.size() != 5) {
			throw new IllegalArgumentException("invalid stroke payload");
		}
		String strokeId = node.get(0).asText();
		if (strokeId == null || strokeId.isBlank()) {
			throw new IllegalArgumentException("invalid stroke id");
		}
		StrokeTool tool = node.get(1).asInt() == 2 ? StrokeTool.ERASER : StrokeTool.PEN;
		int colorIndex = node.get(2).asInt();
		int size = node.get(3).asInt();
		JsonNode pointsNode = node.get(4);
		if (!pointsNode.isArray()) {
			throw new IllegalArgumentException("invalid stroke points");
		}
		List<Stroke.Point> points = new ArrayList<>();
		for (JsonNode pointNode : pointsNode) {
			if (!pointNode.isArray() || pointNode.size() != 2) {
				throw new IllegalArgumentException("invalid point");
			}
			double x = pointNode.get(0).asDouble();
			double y = pointNode.get(1).asDouble();
			if (!hasAtMostScale(x, 5) || !hasAtMostScale(y, 5)) {
				throw new IllegalArgumentException("point precision must be <= 5");
			}
			points.add(new Stroke.Point(x, y));
		}
		return new Stroke(strokeId, tool, colorIndex, size, points);
	}

	private GameSettings parseSettings(JsonNode payload) {
		if (payload == null || !payload.isObject()) {
			throw new IllegalArgumentException("settings payload must be an object");
		}
		int roundCount = payload.path("roundCount").asInt(3);
		int drawSec = payload.path("drawSec").asInt(20);
		int wordChoiceSec = payload.path("wordChoiceSec").asInt(10);
		int wordChoiceCount = payload.path("wordChoiceCount").asInt(3);
		DrawerOrderMode drawerOrderMode = DrawerOrderMode.valueOf(payload.path("drawerOrderMode").asText("JOIN_ORDER"));
		EndMode endMode = EndMode.valueOf(payload.path("endMode").asText("FIRST_CORRECT"));
		if (roundCount < 3 || roundCount > 10) {
			throw new IllegalArgumentException("roundCount must be 3..10");
		}
		if (drawSec < 20 || drawSec > 60) {
			throw new IllegalArgumentException("drawSec must be 20..60");
		}
		if (wordChoiceSec < 5 || wordChoiceSec > 15) {
			throw new IllegalArgumentException("wordChoiceSec must be 5..15");
		}
		if (wordChoiceCount < 3 || wordChoiceCount > 5) {
			throw new IllegalArgumentException("wordChoiceCount must be 3..5");
		}
		return new GameSettings(
			roundCount,
			drawSec,
			wordChoiceSec,
			wordChoiceCount,
			drawerOrderMode,
			endMode
		);
	}

	private String normalizeGuess(String text) {
		return text.toLowerCase()
			.replace(" ", "")
			.replace(".", "")
			.replace(",", "")
			.replace("!", "")
			.replace("?", "");
	}

	private boolean hasAtMostScale(double value, int scale) {
		double multiplier = Math.pow(10, scale);
		double rounded = Math.round(value * multiplier) / multiplier;
		return Math.abs(value - rounded) < 1e-10;
	}

	private boolean allowPerSecond(String key, int limit) {
		long epochSecond = Instant.now().getEpochSecond();
		RateWindow window = rateWindows.get(key, ignored -> new RateWindow(epochSecond));
		synchronized (window) {
			if (window.epochSecond != epochSecond) {
				window.epochSecond = epochSecond;
				window.count = 0;
			}
			if (window.count >= limit) {
				return false;
			}
			window.count++;
			return true;
		}
	}

	private void commitPendingScores(Room room, Turn turn) {
		if (turn.getPendingScores().isEmpty()) {
			return;
		}
		Map<String, Integer> scores = room.getCurrentGame().scores().scores();
		for (Map.Entry<String, Integer> entry : turn.getPendingScores().entrySet()) {
			scores.merge(entry.getKey(), entry.getValue(), Integer::sum);
		}
	}

	private static final class RateWindow {

		private long epochSecond;
		private int count;

		private RateWindow(long epochSecond) {
			this.epochSecond = epochSecond;
			this.count = 0;
		}
	}
}
