package io.jhpark.kopic.ge.api.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.protobuf.Struct;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import io.jhpark.kopic.ge.command.app.CommandValidator;
import io.jhpark.kopic.ge.command.app.GameFlowService;
import io.jhpark.kopic.ge.command.app.InboundRoomEventType;
import io.jhpark.kopic.ge.command.app.RoomEventContext;
import io.jhpark.kopic.ge.command.dto.ClientEnvelope;
import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.command.dto.EngineEnvelopeRequest;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleEvent;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleType;
import io.jhpark.kopic.ge.common.error.EngineRejectedException;
import io.jhpark.kopic.ge.directory.app.SessionPresenceRepository;
import io.jhpark.kopic.ge.rpc.ws.v1.ClientEnvelopeMessage;
import io.jhpark.kopic.ge.rpc.ws.v1.EngineAckMessage;
import io.jhpark.kopic.ge.rpc.ws.v1.EngineAckReasonMessage;
import io.jhpark.kopic.ge.rpc.ws.v1.EngineEnvelopeRequestMessage;
import io.jhpark.kopic.ge.rpc.ws.v1.SessionLifecycleEventMessage;
import io.jhpark.kopic.ge.rpc.ws.v1.SessionLifecycleTypeMessage;
import io.jhpark.kopic.ge.rpc.ws.v1.WsRpcServiceGrpc;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class WsRpcGrpcService extends WsRpcServiceGrpc.WsRpcServiceImplBase {

	private final CommandValidator commandValidator;
	private final GameFlowService gameFlowService;
	private final SessionPresenceRepository sessionPresenceRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public WsRpcGrpcService(
		CommandValidator commandValidator,
		GameFlowService gameFlowService,
		SessionPresenceRepository sessionPresenceRepository
	) {
		this.commandValidator = commandValidator;
		this.gameFlowService = gameFlowService;
		this.sessionPresenceRepository = sessionPresenceRepository;
	}

	@Override
	public void handleEnvelope(EngineEnvelopeRequestMessage request, StreamObserver<EngineAckMessage> responseObserver) {
		EngineAck ack;
		try {
			EngineEnvelopeRequest engineRequest = new EngineEnvelopeRequest(
				request.getRoomId(),
				request.getUserId(),
				toInstant(request.getOccurredAt()),
				toClientEnvelope(request.getEnvelope())
			);
			commandValidator.validateEnvelope(engineRequest.envelope());
			InboundRoomEventType eventType = InboundRoomEventType.fromClientEnvelopeCode(engineRequest.envelope().e());
			RoomEventContext context = new RoomEventContext(
				eventType,
				engineRequest.roomId(),
				engineRequest.userId(),
				engineRequest.occurredAt(),
				engineRequest.envelope().rid(),
				engineRequest.envelope().p()
			);
			ack = gameFlowService.dispatch(context);
		} catch (EngineRejectedException rejected) {
			ack = EngineAck.rejectedAck(rejected.reason());
		} catch (IllegalArgumentException argumentException) {
			ack = EngineAck.rejectedAck(EngineAckReason.REJECTED);
		} catch (Exception exception) {
			ack = EngineAck.rejectedAck(EngineAckReason.INTERNAL_ERROR);
		}
		responseObserver.onNext(toAckMessage(ack));
		responseObserver.onCompleted();
	}

	@Override
	public void handleSessionLifecycle(SessionLifecycleEventMessage request, StreamObserver<EngineAckMessage> responseObserver) {
		EngineAck ack;
		try {
			SessionLifecycleEvent event = new SessionLifecycleEvent(
				request.getRoomId(),
				request.getUserId(),
				toInstant(request.getOccurredAt()),
				toSessionLifecycleType(request.getType()),
				toJsonNode(request.hasPayload() ? request.getPayload() : null)
			);
			syncSessionPresence(event);
			InboundRoomEventType eventType = InboundRoomEventType.fromSessionType(event.type());
			RoomEventContext context = new RoomEventContext(
				eventType,
				event.roomId(),
				event.userId(),
				event.occurredAt(),
				null,
				event.payload() == null ? NullNode.getInstance() : event.payload()
			);
			ack = gameFlowService.dispatch(context);
		} catch (IllegalArgumentException argumentException) {
			ack = EngineAck.rejectedAck(EngineAckReason.REJECTED);
		} catch (Exception exception) {
			ack = EngineAck.rejectedAck(EngineAckReason.INTERNAL_ERROR);
		}
		responseObserver.onNext(toAckMessage(ack));
		responseObserver.onCompleted();
	}

	private void syncSessionPresence(SessionLifecycleEvent event) {
		if (event.type() == SessionLifecycleType.JOIN) {
			String wsNodeId = event.payload() == null ? null : event.payload().path("wsNodeId").asText(null);
			if (wsNodeId != null && !wsNodeId.isBlank()) {
				sessionPresenceRepository.upsert(event.userId(), wsNodeId);
			}
			return;
		}
		if (event.type() == SessionLifecycleType.LEAVE) {
			sessionPresenceRepository.remove(event.userId());
		}
	}

	private ClientEnvelope toClientEnvelope(ClientEnvelopeMessage message) {
		return new ClientEnvelope(
			message.getE(),
			toJsonNode(message.hasP() ? message.getP() : null),
			message.getRid().isBlank() ? null : message.getRid()
		);
	}

	private SessionLifecycleType toSessionLifecycleType(SessionLifecycleTypeMessage type) {
		return switch (type) {
			case JOIN -> SessionLifecycleType.JOIN;
			case LEAVE -> SessionLifecycleType.LEAVE;
			default -> throw new IllegalArgumentException("unsupported session lifecycle type");
		};
	}

	private EngineAckMessage toAckMessage(EngineAck ack) {
		return EngineAckMessage.newBuilder()
			.setAccepted(ack.accepted())
			.setReason(switch (ack.reason()) {
				case ACCEPTED -> EngineAckReasonMessage.ACCEPTED;
				case NOT_OWNER -> EngineAckReasonMessage.NOT_OWNER;
				case MIGRATING -> EngineAckReasonMessage.MIGRATING;
				case REJECTED -> EngineAckReasonMessage.REJECTED;
				case INTERNAL_ERROR -> EngineAckReasonMessage.INTERNAL_ERROR;
			})
			.build();
	}

	private JsonNode toJsonNode(Struct struct) {
		if (struct == null) {
			return NullNode.getInstance();
		}
		try {
			return objectMapper.readTree(JsonFormat.printer().print(struct));
		} catch (IOException exception) {
			throw new IllegalArgumentException("invalid struct payload", exception);
		}
	}

	private Instant toInstant(Timestamp timestamp) {
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}
}
