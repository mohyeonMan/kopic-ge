package io.jhpark.kopic.ge.directory.infra;

import io.jhpark.kopic.ge.directory.app.RoomCodeIndexUpdater;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InMemoryRoomCodeIndexUpdater implements RoomCodeIndexUpdater {

	private final Map<String, String> roomCodeIndex = new ConcurrentHashMap<>();

	@Override
	public void putRoomCode(String roomCode, String roomId) {
		roomCodeIndex.put(roomCode, roomId);
		log.info("room code indexed. roomCode={}, roomId={}", roomCode, roomId);
	}

	@Override
	public void removeRoomCode(String roomCode) {
		roomCodeIndex.remove(roomCode);
		log.info("room code removed. roomCode={}", roomCode);
	}
}
