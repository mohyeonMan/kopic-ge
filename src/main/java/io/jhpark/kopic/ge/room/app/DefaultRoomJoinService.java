package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.ParticipantStatus;
import io.jhpark.kopic.ge.room.domain.Room;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DefaultRoomJoinService implements RoomJoinService {

	private final RoomRegistry roomRegistry;

	public DefaultRoomJoinService(RoomRegistry roomRegistry) {
		this.roomRegistry = roomRegistry;
	}

	@Override
	public Room join(String roomId, String userId, String name) {
		Room room = roomRegistry.findByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));

		Map<String, Participant> participants = new LinkedHashMap<>(room.participants());
		if (!participants.containsKey(userId) && participants.size() >= room.capacity()) {
			throw new IllegalStateException("room is full");
		}
		participants.put(userId, new Participant(userId, name, ParticipantStatus.ACTIVE, null));

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
