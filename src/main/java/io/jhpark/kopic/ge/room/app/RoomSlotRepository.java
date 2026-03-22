package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Room;
import java.util.Optional;

public interface RoomSlotRepository {

	Optional<RoomSlot> findSlotByRoomId(String roomId);

	default Optional<Room> findRoomByRoomId(String roomId) {
		return findSlotByRoomId(roomId).map(RoomSlot::room);
	}

	int countRoomsByOwnerEngineId(String engineId);

	void saveSlot(RoomSlot slot);

	void deleteSlot(String roomId);
}
