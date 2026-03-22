package io.jhpark.kopic.ge.command.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record SessionLifecycleEvent(
	String roomId,
	String userId,
	Instant occurredAt,
	SessionLifecycleType type,
	JsonNode payload
) {
}
