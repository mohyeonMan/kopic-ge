package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.EndMode;
import io.jhpark.kopic.ge.room.domain.GameSettings;
import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.Room;
import io.jhpark.kopic.ge.room.domain.RoomState;
import io.jhpark.kopic.ge.room.domain.RoomType;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DefaultRoomLifecycleService implements RoomLifecycleService {

	private static final int RANDOM_CAPACITY = 8;
	private static final int PRIVATE_MIN_CAPACITY = 2;
	private static final int PRIVATE_MAX_CAPACITY = 8;

	private final RoomRegistry roomRegistry;

	public DefaultRoomLifecycleService(RoomRegistry roomRegistry) {
		this.roomRegistry = roomRegistry;
	}

	@Override
	public Room createPrivateRoom(String engineId, String userId, String name, int capacity) {
		if (capacity < PRIVATE_MIN_CAPACITY || capacity > PRIVATE_MAX_CAPACITY) {
			throw new IllegalArgumentException("private room capacity must be 2..8");
		}
		String roomId = "r-" + UUID.randomUUID().toString().substring(0, 8);
		String roomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
		Map<String, Participant> participants = new LinkedHashMap<>();
		participants.put(userId, new Participant(userId, name));

		Room room = new Room(
			roomId,
			RoomType.PRIVATE,
			roomCode,
			engineId,
			RoomState.LOBBY,
			participants,
			userId,
			new GameSettings(3, 20, GameSettings.FIXED_WORD_CHOICE_SEC, 3, EndMode.FIRST_CORRECT),
			null,
			1L,
			capacity
		);
		roomRegistry.save(room);
		return room;
	}

	@Override
	public Room createRandomRoom(String engineId, String userId, String name) {
		String roomId = "r-" + UUID.randomUUID().toString().substring(0, 8);
		Map<String, Participant> participants = new LinkedHashMap<>();
		participants.put(userId, new Participant(userId, name));

		Room room = new Room(
			roomId,
			RoomType.RANDOM,
			null,
			engineId,
			RoomState.LOBBY,
			participants,
			null,
			new GameSettings(3, 20, GameSettings.FIXED_WORD_CHOICE_SEC, 3, EndMode.FIRST_CORRECT),
			null,
			1L,
			RANDOM_CAPACITY
		);
		roomRegistry.save(room);
		return room;
	}

	@Override
	public void closeRoom(String roomId) {
		roomRegistry.delete(roomId);
	}
}
