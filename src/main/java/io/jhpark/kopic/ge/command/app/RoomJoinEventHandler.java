package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.room.app.RoomJobResult;
import io.jhpark.kopic.ge.room.app.RoomService;
import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.ParticipantStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RoomJoinEventHandler extends AbstractRoomEventHandler {

	public RoomJoinEventHandler(RoomService roomService) {
		super(roomService);
	}

	@Override
	public RoomEventType supports() {
		return RoomEventType.ROOM_JOIN;
	}

	@Override
	public EngineAck handle(RoomEventContext context) {
		try {
			submit(context.roomId(), room -> {
				if (!room.getParticipants().containsKey(context.userId())
					&& room.getParticipants().size() >= room.getCapacity()) {
					throw new IllegalStateException("room is full");
				}
				room.getParticipants().put(
					context.userId(),
					new Participant(context.userId(), context.userId(), ParticipantStatus.ACTIVE, null)
				);
				room.increaseVersion();
				log.info("room join handled. roomId={}, userId={}, participantCount={}",
					context.roomId(), context.userId(), room.getParticipants().size());
				return RoomJobResult.keep();
			});
			return acceptedAck();
		} catch (IllegalStateException exception) {
			return rejectedAck(EngineAckReason.REJECTED);
		}
	}
}
