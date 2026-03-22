package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.room.app.RoomJob;
import io.jhpark.kopic.ge.room.app.RoomService;

public abstract class AbstractRoomEventHandler implements RoomEventHandler {

	private final RoomService roomService;

	protected AbstractRoomEventHandler(RoomService roomService) {
		this.roomService = roomService;
	}

	protected void submit(String roomId, RoomJob roomJob) {
		roomService.submit(roomId, roomJob);
	}

	protected EngineAck acceptedAck() {
		return EngineAck.acceptedAck();
	}

	protected EngineAck rejectedAck(EngineAckReason reason) {
		return EngineAck.rejectedAck(reason);
	}
}
