package io.jhpark.kopic.ge.lobby.dto;

public record CreateRandomRoomCommand(
	String userId,
	String name
) {
}
