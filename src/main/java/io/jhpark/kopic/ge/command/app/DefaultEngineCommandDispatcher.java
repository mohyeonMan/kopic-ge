package io.jhpark.kopic.ge.command.app;

import com.fasterxml.jackson.databind.JsonNode;
import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.command.dto.EngineEnvelopeRequest;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleEvent;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleType;
import io.jhpark.kopic.ge.common.error.EngineRejectedException;
import io.jhpark.kopic.ge.game.domain.Stroke;
import io.jhpark.kopic.ge.game.domain.StrokeTool;
import java.util.ArrayList;
import java.util.List;
import io.jhpark.kopic.ge.room.app.RoomSlotRepository;
import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.ParticipantStatus;
import io.jhpark.kopic.ge.room.domain.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultEngineCommandDispatcher implements EngineCommandDispatcher {

	private static final int EVENT_ROOM_LEAVE = 103;
	private static final int EVENT_GAME_START_REQUEST = 105;
	private static final int EVENT_GAME_SNAPSHOT_REQUEST = 106;
	private static final int EVENT_DRAW_STROKE = 201;
	private static final int EVENT_DRAW_CLEAR = 202;
	private static final int EVENT_GUESS_SUBMIT = 204;

	private final CommandValidator commandValidator;
	private final RoomSlotRepository roomSlotRepository;

	public DefaultEngineCommandDispatcher(
		CommandValidator commandValidator,
		RoomSlotRepository roomSlotRepository
	) {
		this.commandValidator = commandValidator;
		this.roomSlotRepository = roomSlotRepository;
	}

	@Override
	public EngineAck handleEnvelope(EngineEnvelopeRequest request) {
		try {
			commandValidator.validateEnvelope(request.envelope());
			switch (request.envelope().e()) {
				case EVENT_ROOM_LEAVE -> leaveRoom(request.roomId(), request.userId());
				case EVENT_GAME_START_REQUEST, EVENT_GAME_SNAPSHOT_REQUEST, EVENT_DRAW_STROKE, EVENT_DRAW_CLEAR, EVENT_GUESS_SUBMIT ->
					throw new EngineRejectedException("command flow is pending handler migration", EngineAckReason.REJECTED);
				default -> throw new EngineRejectedException("unsupported event", EngineAckReason.REJECTED);
			}
			return EngineAck.acceptedAck();
		} catch (EngineRejectedException rejected) {
			return EngineAck.rejectedAck(rejected.reason());
		} catch (IllegalArgumentException argumentException) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		} catch (Exception exception) {
			return EngineAck.rejectedAck(EngineAckReason.INTERNAL_ERROR);
		}
	}

	@Override
	public EngineAck handleSessionLifecycle(SessionLifecycleEvent event) {
		try {
			if (event.type() == SessionLifecycleType.CONNECTED) {
				joinRoom(event.roomId(), event.userId(), event.userId());
			} else {
				leaveRoom(event.roomId(), event.userId());
			}
			return EngineAck.acceptedAck();
		} catch (IllegalArgumentException exception) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		} catch (Exception exception) {
			return EngineAck.rejectedAck(EngineAckReason.INTERNAL_ERROR);
		}
	}

	private void joinRoom(String roomId, String userId, String name) {
		Room room = roomSlotRepository.findRoomByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));
		if (!room.getParticipants().containsKey(userId) && room.getParticipants().size() >= room.getCapacity()) {
			throw new IllegalStateException("room is full");
		}
		room.getParticipants().put(userId, new Participant(userId, name, ParticipantStatus.ACTIVE, null));
		room.increaseVersion();
		log.info("session connected. roomId={}, userId={}, participantCount={}",
			roomId, userId, room.getParticipants().size());
	}

	private void leaveRoom(String roomId, String userId) {
		Room room = roomSlotRepository.findRoomByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));
		room.getParticipants().remove(userId);
		room.increaseVersion();
		log.info("session disconnected or leave requested. roomId={}, userId={}, participantCount={}",
			roomId, userId, room.getParticipants().size());
	}

	private Stroke toStroke(JsonNode payload) {
		JsonNode node = payload.has("stroke") ? payload.path("stroke") : payload;
		if (node == null || node.isMissingNode() || node.isNull()) {
			throw new IllegalArgumentException("stroke is required");
		}
		if (node.isArray()) {
			return toStrokeFromArray(node);
		}
		return toStrokeFromObject(node);
	}

	private Stroke toStrokeFromArray(JsonNode node) {
		if (node.size() < 5) {
			throw new IllegalArgumentException("stroke array must have 5 elements");
		}
		JsonNode pointsNode = node.get(4);
		if (pointsNode == null || !pointsNode.isArray()) {
			throw new IllegalArgumentException("stroke points must be an array");
		}
		List<Stroke.Point> points = new ArrayList<>(pointsNode.size());
		for (var pair : pointsNode) {
			if (!pair.isArray() || pair.size() < 2) {
				throw new IllegalArgumentException("point must be [x, y]");
			}
			points.add(new Stroke.Point(pair.get(0).asDouble(), pair.get(1).asDouble()));
		}
		return new Stroke(
			node.get(0).asText(),
			toStrokeTool(node.get(1)),
			node.get(2).asInt(),
			node.get(3).asInt(),
			points
		);
	}

	private Stroke toStrokeFromObject(JsonNode node) {
		JsonNode pointsNode = node.path("points");
		if (!pointsNode.isArray()) {
			throw new IllegalArgumentException("stroke.points is required");
		}
		List<Stroke.Point> points = new java.util.ArrayList<>(pointsNode.size());
		for (var pointNode : pointsNode) {
			if (pointNode.isArray() && pointNode.size() >= 2) {
				points.add(new Stroke.Point(pointNode.get(0).asDouble(), pointNode.get(1).asDouble()));
			} else {
				points.add(new Stroke.Point(
					pointNode.path("x").asDouble(),
					pointNode.path("y").asDouble()
				));
			}
		}
		return new Stroke(
			node.path("strokeId").asText(),
			toStrokeTool(node.path("tool")),
			node.path("colorIndex").asInt(),
			node.path("size").asInt(),
			points
		);
	}

	private StrokeTool toStrokeTool(JsonNode node) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return StrokeTool.PEN;
		}
		if (node.isInt() || node.isLong()) {
			int code = node.asInt();
			return switch (code) {
				case 1 -> StrokeTool.PEN;
				case 2 -> StrokeTool.ERASER;
				default -> throw new IllegalArgumentException("unsupported stroke tool: " + code);
			};
		}
		return StrokeTool.valueOf(node.asText("PEN"));
	}
}
