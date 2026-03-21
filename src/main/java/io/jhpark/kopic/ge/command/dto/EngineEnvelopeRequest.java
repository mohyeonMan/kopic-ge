package io.jhpark.kopic.ge.command.dto;

import java.time.Instant;

public record EngineEnvelopeRequest(
	String roomId,
	String userId,
	Instant occurredAt,
	ClientEnvelope envelope
) {
}
