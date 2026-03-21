package io.jhpark.kopic.ge.command.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record ClientEnvelope(
	int e,
	JsonNode p,
	String rid
) {
}
