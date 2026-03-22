package io.jhpark.kopic.ge.command.app;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record RoomEventContext(
	InboundRoomEventType eventType,
	String roomId,
	String userId,
	Instant occurredAt,
	String requestId,
	JsonNode payload
) {
}
