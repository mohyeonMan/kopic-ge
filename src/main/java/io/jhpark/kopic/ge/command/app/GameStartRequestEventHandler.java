package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.room.app.RoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GameStartRequestEventHandler extends AbstractRoomEventHandler {

	public GameStartRequestEventHandler(RoomService roomService) {
		super(roomService);
	}

	@Override
	public RoomEventType supports() {
		return RoomEventType.GAME_START_REQUEST;
	}

	@Override
	public EngineAck handle(RoomEventContext context) {
		log.debug("game start request handler skeleton invoked. roomId={}, userId={}",
			context.roomId(), context.userId());
		return rejectedAck(EngineAckReason.REJECTED);
	}
}
