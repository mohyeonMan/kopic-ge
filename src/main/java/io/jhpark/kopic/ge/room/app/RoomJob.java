package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Room;

@FunctionalInterface
public interface RoomJob {

	RoomJobResult run(Room room);
}
