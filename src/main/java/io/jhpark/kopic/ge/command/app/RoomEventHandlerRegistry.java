package io.jhpark.kopic.ge.command.app;

public interface RoomEventHandlerRegistry {

	RoomEventHandler get(RoomEventType eventType);
}
