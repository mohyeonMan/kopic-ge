package io.jhpark.kopic.ge.command.dto;

import java.time.Instant;

public record SessionLifecycleEvent(
	String roomId,
	String userId,
	Instant occurredAt,
	SessionLifecycleType type
) {
}
