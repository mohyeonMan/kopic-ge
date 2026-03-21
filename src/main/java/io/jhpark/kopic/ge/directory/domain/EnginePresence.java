package io.jhpark.kopic.ge.directory.domain;

import java.time.Instant;

public record EnginePresence(
	String engineId,
	String endpoint,
	EngineStatus status,
	int activeRooms,
	Instant heartbeatUpdatedAt
) {
}
