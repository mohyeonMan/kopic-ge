package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.directory.app.EnginePresencePublisher;
import io.jhpark.kopic.ge.directory.app.RandomJoinableIndexUpdater;
import io.jhpark.kopic.ge.directory.app.RoomCodeIndexUpdater;
import io.jhpark.kopic.ge.directory.app.RoomRoutingUpdater;
import io.jhpark.kopic.ge.directory.domain.EngineStatus;
import io.jhpark.kopic.ge.room.domain.EndMode;
import io.jhpark.kopic.ge.room.domain.GameSettings;
import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.ParticipantStatus;
import io.jhpark.kopic.ge.room.domain.Room;
import io.jhpark.kopic.ge.room.domain.RoomState;
import io.jhpark.kopic.ge.room.domain.RoomType;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultRoomService implements RoomService {

	private static final int RANDOM_CAPACITY = 8;
	private static final int PRIVATE_MIN_CAPACITY = 2;
	private static final int PRIVATE_MAX_CAPACITY = 8;

	private final RoomSlotRepository roomSlotRepository;
	private final RoomRunner roomRunner;
	private final RoomRoutingUpdater roomRoutingUpdater;
	private final RoomCodeIndexUpdater roomCodeIndexUpdater;
	private final RandomJoinableIndexUpdater randomJoinableIndexUpdater;
	private final EnginePresencePublisher enginePresencePublisher;
	private final String engineEndpoint;

	public DefaultRoomService(
		RoomSlotRepository roomSlotRepository,
		RoomRunner roomRunner,
		RoomRoutingUpdater roomRoutingUpdater,
		RoomCodeIndexUpdater roomCodeIndexUpdater,
		RandomJoinableIndexUpdater randomJoinableIndexUpdater,
		EnginePresencePublisher enginePresencePublisher,
		@Value("${engine.endpoint:http://localhost:8080}") String engineEndpoint
	) {
		this.roomSlotRepository = roomSlotRepository;
		this.roomRunner = roomRunner;
		this.roomRoutingUpdater = roomRoutingUpdater;
		this.roomCodeIndexUpdater = roomCodeIndexUpdater;
		this.randomJoinableIndexUpdater = randomJoinableIndexUpdater;
		this.enginePresencePublisher = enginePresencePublisher;
		this.engineEndpoint = engineEndpoint;
	}

	@Override
	public Room createPrivateRoom(String engineId, String userId, String name, int capacity) {
		if (capacity < PRIVATE_MIN_CAPACITY || capacity > PRIVATE_MAX_CAPACITY) {
			throw new IllegalArgumentException("private room capacity must be 2..8");
		}
		String roomId = "r-" + UUID.randomUUID().toString().substring(0, 8);
		String roomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
		Map<String, Participant> participants = new LinkedHashMap<>();
		participants.put(userId, new Participant(userId, name, ParticipantStatus.ACTIVE, null));

		Room room = new Room(
			roomId,
			roomCode,
			RoomType.PRIVATE,
			participants,
			RoomState.LOBBY,
			Instant.now(),
			userId,
			new GameSettings(3, 20, GameSettings.FIXED_WORD_CHOICE_SEC, 3, EndMode.FIRST_CORRECT),
			null,
			engineId,
			1L,
			capacity
		);
		roomSlotRepository.saveSlot(new RoomSlot(room));
		roomRoutingUpdater.putOwnerEngine(room.getRoomId(), engineId);
		roomCodeIndexUpdater.putRoomCode(room.getRoomCode(), room.getRoomId());
		publishPresence(engineId);
		log.info("private room created. roomId={}, ownerEngineId={}, hostUserId={}, capacity={}",
			room.getRoomId(), engineId, userId, capacity);
		return room;
	}

	@Override
	public Room createRandomRoom(String engineId, String userId, String name) {
		String roomId = "r-" + UUID.randomUUID().toString().substring(0, 8);
		Map<String, Participant> participants = new LinkedHashMap<>();
		participants.put(userId, new Participant(userId, name, ParticipantStatus.ACTIVE, null));

		Room room = new Room(
			roomId,
			null,
			RoomType.RANDOM,
			participants,
			RoomState.LOBBY,
			Instant.now(),
			null,
			new GameSettings(3, 20, GameSettings.FIXED_WORD_CHOICE_SEC, 3, EndMode.FIRST_CORRECT),
			null,
			engineId,
			1L,
			RANDOM_CAPACITY
		);
		roomSlotRepository.saveSlot(new RoomSlot(room));
		roomRoutingUpdater.putOwnerEngine(room.getRoomId(), engineId);
		randomJoinableIndexUpdater.addJoinableRoom(room.getRoomId(), room.getParticipants().size());
		publishPresence(engineId);
		log.info("random room created. roomId={}, ownerEngineId={}, userId={}",
			room.getRoomId(), engineId, userId);
		return room;
	}

	@Override
	public void submit(String roomId, RoomJob roomJob) {
		roomRunner.submit(roomId, roomJob);
	}

	@Override
	public void closeRoom(String roomId) {
		String[] ownerEngineHolder = new String[1];
		roomSlotRepository.findRoomByRoomId(roomId).ifPresent(room -> {
			ownerEngineHolder[0] = room.getOwnerEngineId();
			if (room.getRoomCode() != null) {
				roomCodeIndexUpdater.removeRoomCode(room.getRoomCode());
			}
			if (room.getRoomType() == RoomType.RANDOM) {
				randomJoinableIndexUpdater.removeJoinableRoom(roomId);
			}
			roomRoutingUpdater.removeOwnerEngine(roomId);
		});
		log.info("closing room. roomId={}", roomId);
		roomSlotRepository.deleteSlot(roomId);
		if (ownerEngineHolder[0] != null) {
			publishPresence(ownerEngineHolder[0]);
		}
	}

	private void publishPresence(String engineId) {
		enginePresencePublisher.publish(
			engineId,
			engineEndpoint,
			EngineStatus.ACTIVE,
			roomSlotRepository.countRoomsByOwnerEngineId(engineId)
		);
	}
}
