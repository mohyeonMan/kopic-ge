package io.jhpark.kopic.ge.command.app;

import com.fasterxml.jackson.databind.node.NullNode;
import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.command.dto.EngineEnvelopeRequest;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleEvent;
import io.jhpark.kopic.ge.common.error.EngineRejectedException;
import org.springframework.stereotype.Service;

@Service
public class DefaultEngineCommandDispatcher implements EngineCommandDispatcher {

	private final CommandValidator commandValidator;
	private final RoomEventHandlerRegistry roomEventHandlerRegistry;

	public DefaultEngineCommandDispatcher(
		CommandValidator commandValidator,
		RoomEventHandlerRegistry roomEventHandlerRegistry
	) {
		this.commandValidator = commandValidator;
		this.roomEventHandlerRegistry = roomEventHandlerRegistry;
	}

	@Override
	public EngineAck handleEnvelope(EngineEnvelopeRequest request) {
		try {
			commandValidator.validateEnvelope(request.envelope());
			InboundRoomEventType eventType = InboundRoomEventType.fromClientEnvelopeCode(request.envelope().e());
			RoomEventContext context = new RoomEventContext(
				eventType,
				request.roomId(),
				request.userId(),
				request.occurredAt(),
				request.envelope().rid(),
				request.envelope().p()
			);
			return roomEventHandlerRegistry.get(eventType).handle(context);
		} catch (UnsupportedOperationException unsupportedOperationException) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
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
			InboundRoomEventType eventType = InboundRoomEventType.fromSessionType(event.type());
			RoomEventContext context = new RoomEventContext(
				eventType,
				event.roomId(),
				event.userId(),
				event.occurredAt(),
				null,
				NullNode.getInstance()
			);
			return roomEventHandlerRegistry.get(eventType).handle(context);
		} catch (UnsupportedOperationException unsupportedOperationException) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		} catch (IllegalArgumentException exception) {
			return EngineAck.rejectedAck(EngineAckReason.REJECTED);
		} catch (Exception exception) {
			return EngineAck.rejectedAck(EngineAckReason.INTERNAL_ERROR);
		}
	}
}
