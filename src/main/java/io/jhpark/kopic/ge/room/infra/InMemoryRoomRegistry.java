package io.jhpark.kopic.ge.room.infra;

import io.jhpark.kopic.ge.room.app.RoomRegistry;
import io.jhpark.kopic.ge.room.domain.Room;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRoomRegistry implements RoomRegistry {

	private final Map<String, Room> rooms = new ConcurrentHashMap<>();

	@Override
	public Optional<Room> findByRoomId(String roomId) {
		return Optional.ofNullable(rooms.get(roomId));
	}

	@Override
	public void save(Room room) {
		rooms.put(room.roomId(), room);
	}

	@Override
	public void delete(String roomId) {
		rooms.remove(roomId);
	}
}
