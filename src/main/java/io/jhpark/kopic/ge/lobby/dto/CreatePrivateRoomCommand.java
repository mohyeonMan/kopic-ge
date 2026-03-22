package io.jhpark.kopic.ge.lobby.dto;

public record CreatePrivateRoomCommand(
	String userId,
	String nickname,
	int capacity
) {
}
