package io.jhpark.kopic.ge.outbound.dto;

public record TargetedDelivery(
	String userId,
	ServerEnvelope envelope
) {
}
