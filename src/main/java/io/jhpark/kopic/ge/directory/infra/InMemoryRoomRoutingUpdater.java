package io.jhpark.kopic.ge.directory.infra;

import io.jhpark.kopic.ge.directory.app.RoomRoutingUpdater;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InMemoryRoomRoutingUpdater implements RoomRoutingUpdater {

	private final Map<String, String> roomOwners = new ConcurrentHashMap<>();

	@Override
	public void putOwnerEngine(String roomId, String engineId) {
		roomOwners.put(roomId, engineId);
		log.info("room owner indexed. roomId={}, engineId={}", roomId, engineId);
	}

	@Override
	public void removeOwnerEngine(String roomId) {
		roomOwners.remove(roomId);
		log.info("room owner removed. roomId={}", roomId);
	}
}
