package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Room;
import java.util.Optional;

public interface RoomRegistry {

	Optional<Room> findByRoomId(String roomId);

	void save(Room room);

	void delete(String roomId);
}
