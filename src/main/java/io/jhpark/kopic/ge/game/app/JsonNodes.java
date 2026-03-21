package io.jhpark.kopic.ge.game.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

final class JsonNodes {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private JsonNodes() {
	}

	static ObjectNode obj() {
		return MAPPER.createObjectNode();
	}
}
