package io.jhpark.kopic.ge.lobby.dto;

public record CreatePrivateRoomCommand(
	String userId,
	String name,
	int capacity
) {
}
