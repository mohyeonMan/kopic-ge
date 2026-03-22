package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.room.app.RoomRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DrawClearEventHandler extends AbstractRoomEventHandler {

	public DrawClearEventHandler(RoomRunner roomRunner) {
		super(roomRunner);
	}

	@Override
	public RoomEventType supports() {
		return RoomEventType.DRAW_CLEAR;
	}

	@Override
	public EngineAck handle(RoomEventContext context) {
		log.debug("draw clear handler skeleton invoked. roomId={}, userId={}",
			context.roomId(), context.userId());
		return rejectedAck(EngineAckReason.REJECTED);
	}
}
