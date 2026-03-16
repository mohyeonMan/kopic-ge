package io.jhpark.kopic.ge.game.app;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jhpark.kopic.ge.game.domain.CanvasState;
import io.jhpark.kopic.ge.game.domain.GameRuntime;
import io.jhpark.kopic.ge.game.domain.GameStatus;
import io.jhpark.kopic.ge.game.domain.RoundRuntime;
import io.jhpark.kopic.ge.game.domain.RoundState;
import io.jhpark.kopic.ge.game.domain.ScoreBoard;
import io.jhpark.kopic.ge.game.domain.TurnPhase;
import io.jhpark.kopic.ge.game.domain.TurnRuntime;
import io.jhpark.kopic.ge.outbound.app.AudienceResolver;
import io.jhpark.kopic.ge.outbound.app.OutboundPublisher;
import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import io.jhpark.kopic.ge.room.app.RoomRegistry;
import io.jhpark.kopic.ge.room.domain.GameSettings;
import io.jhpark.kopic.ge.room.domain.Room;
import io.jhpark.kopic.ge.room.domain.RoomState;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DefaultGameStartService implements GameStartService {

	private final RoomRegistry roomRegistry;
	private final OutboundPublisher outboundPublisher;
	private final AudienceResolver audienceResolver;

	public DefaultGameStartService(
		RoomRegistry roomRegistry,
		OutboundPublisher outboundPublisher,
		AudienceResolver audienceResolver
	) {
		this.roomRegistry = roomRegistry;
		this.outboundPublisher = outboundPublisher;
		this.audienceResolver = audienceResolver;
	}

	@Override
	public void startGame(String roomId, String userId, GameSettings settings, String requestId) {
		Room room = roomRegistry.findByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));
		if (room.roomType().name().equals("PRIVATE") && !userId.equals(room.hostUserId())) {
			throw new IllegalStateException("only host can start private game");
		}
		if (room.game() != null && room.game().status() == GameStatus.RUNNING) {
			throw new IllegalStateException("game already started");
		}

		Instant now = Instant.now();
		String drawerUserId = room.participants().values().stream()
			.findFirst()
			.map(participant -> participant.userId())
			.orElseThrow(() -> new IllegalStateException("no participant in room"));

		Map<String, Integer> scores = new LinkedHashMap<>();
		room.participants().values().forEach(participant -> scores.put(participant.userId(), 0));

		TurnRuntime turn = new TurnRuntime(
			"t-" + now.toEpochMilli(),
			drawerUserId,
			"apple",
			now,
			now.plusSeconds(settings.drawSec()),
			new CanvasState(List.of()),
			TurnPhase.DRAWING
		);

		GameRuntime gameRuntime = new GameRuntime(
			"g-" + now.toEpochMilli(),
			GameStatus.RUNNING,
			settings,
			new RoundRuntime(1, RoundState.RUNNING, now, null, 1),
			1,
			turn,
			new ScoreBoard(scores),
			Set.of(),
			null
		);

		Room updated = new Room(
			room.roomId(),
			room.roomType(),
			room.roomCode(),
			room.ownerEngineId(),
			RoomState.RUNNING,
			room.participants(),
			room.hostUserId(),
			settings,
			gameRuntime,
			room.version() + 1,
			room.capacity()
		);
		roomRegistry.save(updated);

		ObjectNode startedPayload = JsonNodes.obj()
			.put("gameId", gameRuntime.gameId());
		ObjectNode turnPayload = JsonNodes.obj()
			.put("gameId", gameRuntime.gameId())
			.put("round", 1)
			.put("turn", 1)
			.put("turnId", turn.turnId())
			.put("drawerUserId", turn.drawerUserId())
			.put("durationSec", settings.drawSec());

		outboundPublisher.publish(audienceResolver.resolve(updated, new ServerEnvelope(302, startedPayload, requestId)));
		outboundPublisher.publish(audienceResolver.resolve(updated, new ServerEnvelope(303, JsonNodes.obj().put("gameId", gameRuntime.gameId()).put("round", 1), requestId)));
		outboundPublisher.publish(audienceResolver.resolve(updated, new ServerEnvelope(304, turnPayload, requestId)));
	}
}
