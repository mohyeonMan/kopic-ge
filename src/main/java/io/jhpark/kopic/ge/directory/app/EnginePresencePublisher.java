package io.jhpark.kopic.ge.directory.app;

import io.jhpark.kopic.ge.directory.domain.EngineStatus;

public interface EnginePresencePublisher {

	void publish(String engineId, String endpoint, EngineStatus status, int activeRooms);
}
