package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.Room;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DefaultRoomLeaveService implements RoomLeaveService {

	private final RoomRegistry roomRegistry;

	public DefaultRoomLeaveService(RoomRegistry roomRegistry) {
		this.roomRegistry = roomRegistry;
	}

	@Override
	public Room leave(String roomId, String userId) {
		Room room = roomRegistry.findByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));

		Map<String, Participant> participants = new LinkedHashMap<>(room.participants());
		participants.remove(userId);

		Room updated = new Room(
			room.roomId(),
			room.roomCode(),
			room.roomType(),
			participants,
			room.state(),
			room.createdAt(),
			room.hostUserId(),
			room.settings(),
			room.currentGame(),
			room.ownerEngineId(),
			room.version() + 1,
			room.capacity()
		);
		roomRegistry.save(updated);
		return updated;
	}
}
