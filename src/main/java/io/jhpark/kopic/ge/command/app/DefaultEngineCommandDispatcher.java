package io.jhpark.kopic.ge.command.app;

import com.fasterxml.jackson.databind.JsonNode;
import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.command.dto.EngineEnvelopeRequest;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleEvent;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleType;
import io.jhpark.kopic.ge.common.error.EngineRejectedException;
import io.jhpark.kopic.ge.game.app.DrawCommandService;
import io.jhpark.kopic.ge.game.app.GameStartService;
import io.jhpark.kopic.ge.game.app.GuessCommandService;
import io.jhpark.kopic.ge.game.app.SnapshotService;
import io.jhpark.kopic.ge.game.domain.Stroke;
import io.jhpark.kopic.ge.game.domain.StrokeTool;
import io.jhpark.kopic.ge.outbound.app.OutboundPublisher;
import io.jhpark.kopic.ge.outbound.dto.TargetedDelivery;
import io.jhpark.kopic.ge.room.app.RoomJoinService;
import io.jhpark.kopic.ge.room.app.RoomLeaveService;
import io.jhpark.kopic.ge.room.domain.EndMode;
import io.jhpark.kopic.ge.room.domain.GameSettings;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultEngineCommandDispatcher implements EngineCommandDispatcher {

	private static final int EVENT_ROOM_LEAVE = 103;
	private static final int EVENT_GAME_START_REQUEST = 105;
	private static final int EVENT_GAME_SNAPSHOT_REQUEST = 106;
	private static final int EVENT_DRAW_STROKE = 201;
	private static final int EVENT_DRAW_CLEAR = 202;
	private static final int EVENT_GUESS_SUBMIT = 204;

	private final CommandValidator commandValidator;
	private final RoomJoinService roomJoinService;
	private final RoomLeaveService roomLeaveService;
	private final GameStartService gameStartService;
	private final DrawCommandService drawCommandService;
	private final GuessCommandService guessCommandService;
	private final SnapshotService snapshotService;
	private final OutboundPublisher outboundPublisher;

	public DefaultEngineCommandDispatcher(
		CommandValidator commandValidator,
		RoomJoinService roomJoinService,
		RoomLeaveService roomLeaveService,
		GameStartService gameStartService,
		DrawCommandService drawCommandService,
		GuessCommandService guessCommandService,
		SnapshotService snapshotService,
		OutboundPublisher outboundPublisher
	) {
		this.commandValidator = commandValidator;
		this.roomJoinService = roomJoinService;
		this.roomLeaveService = roomLeaveService;
		this.gameStartService = gameStartService;
		this.drawCommandService = drawCommandService;
		this.guessCommandService = guessCommandService;
		this.snapshotService = snapshotService;
		this.outboundPublisher = outboundPublisher;
	}

	@Override
	public EngineAck handleEnvelope(EngineEnvelopeRequest request) {
		try {
			commandValidator.validateEnvelope(request.envelope());
			switch (request.envelope().e()) {
				case EVENT_ROOM_LEAVE -> roomLeaveService.leave(request.roomId(), request.userId());
				case EVENT_GAME_START_REQUEST -> {
					var payload = request.envelope().p();
					GameSettings settings = new GameSettings(
						payload.path("roundCount").asInt(3),
						payload.path("drawSec").asInt(20),
						GameSettings.FIXED_WORD_CHOICE_SEC,
						payload.path("wordChoiceCount").asInt(3),
						EndMode.valueOf(payload.path("endMode").asText("FIRST_CORRECT"))
					);
					gameStartService.startGame(request.roomId(), request.userId(), settings, request.envelope().rid());
				}
				case EVENT_GAME_SNAPSHOT_REQUEST -> outboundPublisher.publish(List.of(
					new TargetedDelivery(request.userId(), snapshotService.buildSnapshot(request.roomId(), request.envelope().rid()))
				));
				case EVENT_DRAW_STROKE -> {
					var payload = request.envelope().p();
					Stroke stroke = toStroke(payload);
					drawCommandService.drawStroke(
						request.roomId(),
						request.userId(),
						payload.path("turnId").asText(),
						stroke,
						request.envelope().rid()
					);
				}
				case EVENT_DRAW_CLEAR -> {
					var payload = request.envelope().p();
					drawCommandService.clearCanvas(
						request.roomId(),
						request.userId(),
						payload.path("turnId").asText(),
						request.envelope().rid()
					);
				}
				case EVENT_GUESS_SUBMIT -> guessCommandService.submitGuess(
					request.roomId(),
					request.userId(),
					request.envelope().p().path("text").asText(),
					request.envelope().rid()
				);
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
				roomJoinService.join(event.roomId(), event.userId(), event.userId());
				outboundPublisher.publish(List.of(
					new TargetedDelivery(event.userId(), snapshotService.buildSnapshot(event.roomId(), null))
				));
			} else {
				roomLeaveService.leave(event.roomId(), event.userId());
			}
			return EngineAck.acceptedAck();
		} catch (IllegalArgumentException exception) {
			return EngineAck.rejectedAck(EngineAckReason.NOT_OWNER);
		} catch (Exception exception) {
			return EngineAck.rejectedAck(EngineAckReason.INTERNAL_ERROR);
		}
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
