package io.jhpark.kopic.ge.lobby.dto;

public record ReassignRoomResponse(
	String roomId,
	String sourceEngineId,
	String targetEngineId,
	String targetEngineEndpoint,
	boolean reassigned
) {
}
