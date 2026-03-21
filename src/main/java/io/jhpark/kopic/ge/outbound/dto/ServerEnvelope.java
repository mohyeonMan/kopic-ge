package io.jhpark.kopic.ge.outbound.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ServerEnvelope(
	int e,
	JsonNode p,
	String rid
) {
}
