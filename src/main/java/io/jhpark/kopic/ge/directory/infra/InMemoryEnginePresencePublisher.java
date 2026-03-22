package io.jhpark.kopic.ge.directory.infra;

import io.jhpark.kopic.ge.directory.app.EnginePresencePublisher;
import io.jhpark.kopic.ge.directory.domain.EnginePresence;
import io.jhpark.kopic.ge.directory.domain.EngineStatus;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InMemoryEnginePresencePublisher implements EnginePresencePublisher {

	private final Map<String, EnginePresence> presences = new ConcurrentHashMap<>();

	@Override
	public void publish(String engineId, String endpoint, EngineStatus status, int activeRooms) {
		presences.put(engineId, new EnginePresence(engineId, endpoint, status, activeRooms, Instant.now()));
		log.info("engine presence published. engineId={}, status={}, activeRooms={}",
			engineId, status, activeRooms);
	}
}
