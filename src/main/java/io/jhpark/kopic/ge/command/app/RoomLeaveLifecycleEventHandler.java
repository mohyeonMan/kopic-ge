package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.room.app.RoomJobResult;
import io.jhpark.kopic.ge.room.app.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoomLeaveLifecycleEventHandler extends AbstractRoomEventHandler {

	public RoomLeaveLifecycleEventHandler(RoomService roomService) {
		super(roomService);
	}

	@Override
	public InboundRoomEventType supports() {
		return InboundRoomEventType.ROOM_LEAVE;
	}

	@Override
	public EngineAck handle(RoomEventContext context) {
		submit(context.roomId(), room -> {
			room.getParticipants().remove(context.userId());
			room.increaseVersion();
			log.info("room leave handled. roomId={}, userId={}, participantCount={}",
				context.roomId(), context.userId(), room.getParticipants().size());
			return RoomJobResult.keep();
		});
		return acceptedAck();
	}
}
