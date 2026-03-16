package io.jhpark.kopic.ge.game.app;

import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import io.jhpark.kopic.ge.room.app.RoomRegistry;
import io.jhpark.kopic.ge.room.domain.Room;
import org.springframework.stereotype.Service;

@Service
public class DefaultSnapshotService implements SnapshotService {

	private final RoomRegistry roomRegistry;

	public DefaultSnapshotService(RoomRegistry roomRegistry) {
		this.roomRegistry = roomRegistry;
	}

	@Override
	public ServerEnvelope buildSnapshot(String roomId, String requestId) {
		Room room = roomRegistry.findByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));

		var payload = JsonNodes.obj();
		var roomNode = payload.putObject("room");
		roomNode.put("roomId", room.roomId());
		roomNode.put("roomType", room.roomType().name().toLowerCase());
		if (room.roomCode() != null) {
			roomNode.put("roomCode", room.roomCode());
		}
		if (room.hostUserId() != null) {
			roomNode.put("hostUserId", room.hostUserId());
		}

		var participantsNode = payload.putArray("participants");
		room.participants().values().forEach(participant -> {
			var participantNode = participantsNode.addObject();
			participantNode.put("userId", participant.userId());
			participantNode.put("name", participant.name());
		});

		if (room.game() != null) {
			var gameNode = payload.putObject("game");
			gameNode.put("status", room.game().status().name());
			gameNode.put("gameId", room.game().gameId());
			gameNode.put("round", room.game().round().roundNo());
			gameNode.put("turn", room.game().turn());
			if (room.game().turnState() != null) {
				gameNode.put("turnId", room.game().turnState().turnId());
				gameNode.put("drawerUserId", room.game().turnState().drawerUserId());
			}
		}

		var canvasNode = payload.putObject("canvas");
		var strokesNode = canvasNode.putArray("strokes");
		if (room.game() != null && room.game().turnState() != null) {
			room.game().turnState().canvas().strokes().forEach(stroke -> {
				var compactStroke = strokesNode.addArray();
				compactStroke.add(stroke.strokeId());
				compactStroke.add(stroke.tool().ordinal() + 1);
				compactStroke.add(stroke.colorIndex());
				compactStroke.add(stroke.size());
				var pointsNode = compactStroke.addArray();
				stroke.points().forEach(point -> {
					var pair = pointsNode.addArray();
					pair.add(point.x());
					pair.add(point.y());
				});
			});
		}

		return new ServerEnvelope(408, payload, requestId);
	}
}
