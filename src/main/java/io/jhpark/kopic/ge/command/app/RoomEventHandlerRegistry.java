package io.jhpark.kopic.ge.command.app;

public interface RoomEventHandlerRegistry {

	RoomEventHandler get(InboundRoomEventType eventType);
}
