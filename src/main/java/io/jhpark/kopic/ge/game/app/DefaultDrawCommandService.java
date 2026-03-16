package io.jhpark.kopic.ge.game.app;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jhpark.kopic.ge.game.domain.CanvasState;
import io.jhpark.kopic.ge.game.domain.GameRuntime;
import io.jhpark.kopic.ge.game.domain.GameStatus;
import io.jhpark.kopic.ge.game.domain.Stroke;
import io.jhpark.kopic.ge.game.domain.TurnRuntime;
import io.jhpark.kopic.ge.outbound.app.AudienceResolver;
import io.jhpark.kopic.ge.outbound.app.OutboundPublisher;
import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import io.jhpark.kopic.ge.room.app.RoomRegistry;
import io.jhpark.kopic.ge.room.domain.Room;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultDrawCommandService implements DrawCommandService {

	private final RoomRegistry roomRegistry;
	private final OutboundPublisher outboundPublisher;
	private final AudienceResolver audienceResolver;

	public DefaultDrawCommandService(
		RoomRegistry roomRegistry,
		OutboundPublisher outboundPublisher,
		AudienceResolver audienceResolver
	) {
		this.roomRegistry = roomRegistry;
		this.outboundPublisher = outboundPublisher;
		this.audienceResolver = audienceResolver;
	}

	@Override
	public void drawStroke(String roomId, String userId, String turnId, Stroke stroke, String requestId) {
		Room room = roomRegistry.findByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));
		GameRuntime game = requireRunningGame(room);
		TurnRuntime turn = game.turnState();
		if (!turn.drawerUserId().equals(userId)) {
			throw new IllegalStateException("not drawer");
		}
		if (!turn.turnId().equals(turnId)) {
			throw new IllegalStateException("turn mismatch");
		}

		List<Stroke> strokes = new ArrayList<>(turn.canvas().strokes());
		strokes.add(stroke);

		TurnRuntime updatedTurn = new TurnRuntime(
			turn.turnId(),
			turn.drawerUserId(),
			turn.secretWord(),
			turn.startedAt(),
			turn.endsAt(),
			new CanvasState(strokes),
			turn.phase()
		);
		GameRuntime updatedGame = new GameRuntime(
			game.gameId(),
			game.status(),
			game.settings(),
			game.round(),
			game.turn(),
			updatedTurn,
			game.scores(),
			game.correctUsersInTurn(),
			game.resultViewUntil()
		);
		Room updatedRoom = new Room(
			room.roomId(),
			room.roomType(),
			room.roomCode(),
			room.ownerEngineId(),
			room.state(),
			room.participants(),
			room.hostUserId(),
			room.settings(),
			updatedGame,
			room.version() + 1,
			room.capacity()
		);
		roomRegistry.save(updatedRoom);

		ObjectNode payload = JsonNodes.obj()
			.put("turnId", turnId)
			.put("drawerUserId", userId);
		ArrayNode compactStroke = payload.putArray("stroke");
		compactStroke.add(stroke.strokeId());
		compactStroke.add(stroke.tool().ordinal() + 1);
		compactStroke.add(stroke.colorIndex());
		compactStroke.add(stroke.size());
		ArrayNode points = compactStroke.addArray();
		stroke.points().forEach(point -> {
			ArrayNode pair = points.addArray();
			pair.add(point.x());
			pair.add(point.y());
		});
		outboundPublisher.publish(audienceResolver.resolve(updatedRoom, new ServerEnvelope(401, payload, requestId)));
	}

	@Override
	public void clearCanvas(String roomId, String userId, String turnId, String requestId) {
		Room room = roomRegistry.findByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));
		GameRuntime game = requireRunningGame(room);
		TurnRuntime turn = game.turnState();
		if (!turn.drawerUserId().equals(userId)) {
			throw new IllegalStateException("not drawer");
		}

		TurnRuntime updatedTurn = new TurnRuntime(
			turn.turnId(),
			turn.drawerUserId(),
			turn.secretWord(),
			turn.startedAt(),
			turn.endsAt(),
			new CanvasState(List.of()),
			turn.phase()
		);
		GameRuntime updatedGame = new GameRuntime(
			game.gameId(),
			game.status(),
			game.settings(),
			game.round(),
			game.turn(),
			updatedTurn,
			game.scores(),
			game.correctUsersInTurn(),
			game.resultViewUntil()
		);
		Room updatedRoom = new Room(
			room.roomId(), room.roomType(), room.roomCode(), room.ownerEngineId(), room.state(), room.participants(),
			room.hostUserId(), room.settings(), updatedGame, room.version() + 1, room.capacity()
		);
		roomRegistry.save(updatedRoom);

		ObjectNode payload = JsonNodes.obj()
			.put("turnId", turnId)
			.put("drawerUserId", userId);
		outboundPublisher.publish(audienceResolver.resolve(updatedRoom, new ServerEnvelope(402, payload, requestId)));
	}

	private GameRuntime requireRunningGame(Room room) {
		if (room.game() == null || room.game().status() != GameStatus.RUNNING) {
			throw new IllegalStateException("game not running");
		}
		return room.game();
	}
}
