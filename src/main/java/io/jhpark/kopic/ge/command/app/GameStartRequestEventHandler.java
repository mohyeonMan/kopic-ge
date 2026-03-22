package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.game.domain.CanvasState;
import io.jhpark.kopic.ge.game.domain.Game;
import io.jhpark.kopic.ge.game.domain.GameStatus;
import io.jhpark.kopic.ge.game.domain.Round;
import io.jhpark.kopic.ge.game.domain.RoundState;
import io.jhpark.kopic.ge.game.domain.ScoreBoard;
import io.jhpark.kopic.ge.game.domain.Turn;
import io.jhpark.kopic.ge.game.domain.TurnState;
import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.room.app.RoomJobResult;
import io.jhpark.kopic.ge.room.app.RoomService;
import io.jhpark.kopic.ge.room.domain.EndMode;
import io.jhpark.kopic.ge.room.domain.GameSettings;
import io.jhpark.kopic.ge.room.domain.RoomState;
import io.jhpark.kopic.ge.room.domain.RoomType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
				GameSettings.FIXED_WORD_CHOICE_SEC,
				context.payload().path("wordChoiceCount").asInt(3),
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
			if (room.getParticipants().isEmpty()) {
				log.info("game start ignored because room is empty. roomId={}", context.roomId());
				return RoomJobResult.keep();
			}

			Instant now = Instant.now();
			String drawerUserId = room.getParticipants().keySet().iterator().next();
			List<String> wordChoices = createWordChoices(room.getRoomId(), requestedSettings.wordChoiceCount());

			Turn turn = new Turn(
				nextTurnId(),
				drawerUserId,
				null,
				wordChoices,
				TurnState.WORD_CHOICE,
				new LinkedHashSet<>(),
				null,
				new CanvasState(new ArrayList<>()),
				now,
				now.plusSeconds(requestedSettings.wordChoiceSec()),
				null
			);
			Round round = new Round(
				1,
				RoundState.RUNNING,
				0,
				turn,
				now,
				null
			);
			Game game = new Game(
				nextGameId(),
				room.getRoomId(),
				GameStatus.RUNNING,
				requestedSettings,
				new ScoreBoard(initialScores(room)),
				round,
				now,
				null,
				null
			);

			room.setSettings(requestedSettings);
			room.setCurrentGame(game);
			room.setState(RoomState.RUNNING);
			room.increaseVersion();

			log.info("game started. roomId={}, userId={}, gameId={}, drawerUserId={}, roundCount={}",
				context.roomId(), context.userId(), game.gameId(), drawerUserId, requestedSettings.roundCount());
			return RoomJobResult.keepWith(
				WordChoiceFlowSupport.timeoutFollowUp(turn.getTurnId(), requestedSettings.wordChoiceSec())
			);
		});
		return acceptedAck();
	}

	private Map<String, Integer> initialScores(io.jhpark.kopic.ge.room.domain.Room room) {
		Map<String, Integer> scores = new LinkedHashMap<>();
		for (String userId : room.getParticipants().keySet()) {
			scores.put(userId, 0);
		}
		return scores;
	}

	private List<String> createWordChoices(String roomId, int count) {
		int start = Math.floorMod(roomId.hashCode(), WORD_POOL.size());
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
