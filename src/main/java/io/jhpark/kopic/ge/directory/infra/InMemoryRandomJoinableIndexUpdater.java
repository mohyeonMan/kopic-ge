package io.jhpark.kopic.ge.directory.infra;

import io.jhpark.kopic.ge.directory.app.RandomJoinableIndexUpdater;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InMemoryRandomJoinableIndexUpdater implements RandomJoinableIndexUpdater {

	private final Map<String, Integer> joinableRooms = new ConcurrentHashMap<>();

	@Override
	public void addJoinableRoom(String roomId, int score) {
		joinableRooms.put(roomId, score);
		log.info("random joinable room added. roomId={}, score={}", roomId, score);
	}

	@Override
	public void removeJoinableRoom(String roomId) {
		joinableRooms.remove(roomId);
		log.info("random joinable room removed. roomId={}", roomId);
	}
}
