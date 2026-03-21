package io.jhpark.kopic.ge.room.infra;

import io.jhpark.kopic.ge.room.app.RoomSlot;
import io.jhpark.kopic.ge.room.app.RoomSlotRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InMemoryRoomSlotRepository implements RoomSlotRepository {

	private final Map<String, RoomSlot> slots = new ConcurrentHashMap<>();

	@Override
	public Optional<RoomSlot> findSlotByRoomId(String roomId) {
		log.debug("finding room slot. roomId={}", roomId);
		return Optional.ofNullable(slots.get(roomId));
	}

	@Override
	public void saveSlot(RoomSlot slot) {
		log.debug("saving room slot. roomId={}", slot.roomId());
		slots.put(slot.roomId(), slot);
	}

	@Override
	public void deleteSlot(String roomId) {
		log.info("deleting room slot. roomId={}", roomId);
		slots.remove(roomId);
	}
}
