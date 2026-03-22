package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.game.domain.CanvasState;
import io.jhpark.kopic.ge.game.domain.DrawingPhase;
import io.jhpark.kopic.ge.game.domain.Game;
import io.jhpark.kopic.ge.game.domain.GameStatus;
import io.jhpark.kopic.ge.game.domain.Round;
import io.jhpark.kopic.ge.game.domain.RoundState;
import io.jhpark.kopic.ge.game.domain.ScoreBoard;
import io.jhpark.kopic.ge.game.domain.Turn;
import io.jhpark.kopic.ge.game.domain.TurnPhase;
import io.jhpark.kopic.ge.game.domain.TurnState;
import io.jhpark.kopic.ge.game.domain.WordChoicePhase;
import io.jhpark.kopic.ge.room.domain.DrawerOrderMode;
import io.jhpark.kopic.ge.room.app.RoomFollowUp;
import io.jhpark.kopic.ge.room.app.RoomJob;
import io.jhpark.kopic.ge.room.app.RoomJobResult;
import io.jhpark.kopic.ge.room.app.RoomService;
import io.jhpark.kopic.ge.room.domain.EndMode;
import io.jhpark.kopic.ge.room.domain.GameSettings;
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
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameStartRequestEventHandler extends AbstractRoomEventHandler {

	private static final List<String> WORD_POOL = List.of(
		"apple", "banana", "cat", "train", "ocean",
		"camera", "bridge", "guitar", "rocket", "pencil"
	);

	public GameStartRequestEventHandler(RoomService roomService) {
		super(roomService);
	}

	@Override
	public InboundRoomEventType supports() {
		return InboundRoomEventType.GAME_START_REQUEST;
	}

	@Override
	public EngineAck handle(RoomEventContext context) {
		GameSettings requestedSettings;
		try {
			requestedSettings = new GameSettings(
				context.payload().path("roundCount").asInt(3),
				context.payload().path("drawSec").asInt(20),
				context.payload().path("wordChoiceSec").asInt(10),
				context.payload().path("wordChoiceCount").asInt(3),
				resolveDrawerOrderMode(context),
				EndMode.valueOf(context.payload().path("endMode").asText("FIRST_CORRECT"))
			);
		} catch (IllegalArgumentException exception) {
			return rejectedAck(EngineAckReason.REJECTED);
		}

		submit(context.roomId(), room -> {
			if (room.getRoomType() != RoomType.PRIVATE) {
				log.info("game start ignored for non-private room. roomId={}, userId={}, roomType={}",
					context.roomId(), context.userId(), room.getRoomType());
				return RoomJobResult.keep();
			}
			if (room.getHostUserId() == null || !room.getHostUserId().equals(context.userId())) {
				log.info("game start ignored for non-host user. roomId={}, userId={}, hostUserId={}",
					context.roomId(), context.userId(), room.getHostUserId());
				return RoomJobResult.keep();
			}
			if (room.getCurrentGame() != null || room.getState() != RoomState.LOBBY) {
				log.info("game start ignored because room is not startable. roomId={}, state={}, hasGame={}",
					context.roomId(), room.getState(), room.getCurrentGame() != null);
				return RoomJobResult.keep();
			}
			if (room.getParticipants().size() < 2) {
				log.info("game start ignored because participant count is insufficient. roomId={}, participantCount={}",
					context.roomId(), room.getParticipants().size());
				return RoomJobResult.keep();
			}

			RoomJobResult result = startGame(room, requestedSettings, Instant.now());
			log.info("game start accepted. roomId={}, userId={}, roundCount={}, drawSec={}, wordChoiceCount={}",
				context.roomId(), context.userId(), requestedSettings.roundCount(),
				requestedSettings.drawSec(), requestedSettings.wordChoiceCount());
			return result;
		});
		return acceptedAck();
	}

	private RoomJobResult startGame(Room room, GameSettings requestedSettings, Instant now) {
		Game game = new Game(
			nextGameId(),
			room.getRoomId(),
			GameStatus.RUNNING,
			requestedSettings,
			new ScoreBoard(initialScores(room)),
			null,
			now,
			null,
			null
		);

		room.setSettings(requestedSettings);
		room.setCurrentGame(game);
		room.setState(RoomState.RUNNING);
		room.increaseVersion();

		return RoomJobResult.keepWith(
			RoomFollowUp.delayed(Duration.ofSeconds(2), startRound(1))
		);
	}

	private RoomJob startRound(int roundNo) {
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

			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(Duration.ofSeconds(2), startWordChoiceTurn(roundNo, 0))
			);
		};
	}

	private RoomJob startWordChoiceTurn(int roundNo, int turnCursor) {
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
				new WordChoicePhase(
					createWordChoices(room.getRoomId(), game.settings().wordChoiceCount()),
					now,
					now.plusSeconds(game.settings().wordChoiceSec())
				)
			);

			round.setTurnCursor(turnCursor);
			round.setCurrentTurn(turn);
			room.increaseVersion();

			return RoomJobResult.keepWith(
				RoomFollowUp.delayed(
					Duration.ofSeconds(game.settings().wordChoiceSec()),
					wordChoiceTimeout(turn.getTurnId())
				)
			);
		};
	}

	private RoomJob wordChoiceTimeout(String expectedTurnId) {
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
			return RoomJobResult.keep();
		};
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

	private DrawerOrderMode resolveDrawerOrderMode(RoomEventContext context) {
		String raw = context.payload().path("drawerOrderMode").asText("JOIN_ORDER");
		return DrawerOrderMode.valueOf(raw);
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

	private WordChoicePhase requireWordChoicePhase(TurnPhase phase) {
		if (phase instanceof WordChoicePhase wordChoicePhase) {
			return wordChoicePhase;
		}
		throw new IllegalStateException("turn phase must be WordChoicePhase");
	}

	private Map<String, Integer> initialScores(Room room) {
		Map<String, Integer> scores = new LinkedHashMap<>();
		for (String userId : room.getParticipants().keySet()) {
			scores.put(userId, 0);
		}
		return scores;
	}

	private List<String> createWordChoices(String roomId, int count) {
		int start = Math.floorMod(roomId.hashCode() + ThreadLocalRandom.current().nextInt(), WORD_POOL.size());
		List<String> choices = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			choices.add(WORD_POOL.get((start + i) % WORD_POOL.size()));
		}
		return choices;
	}

	private String nextGameId() {
		return "g-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private String nextTurnId() {
		return "t-" + UUID.randomUUID().toString().substring(0, 8);
	}
}
