package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;

public interface RoomEventHandler {

	RoomEventType supports();

	EngineAck handle(RoomEventContext context);
}
