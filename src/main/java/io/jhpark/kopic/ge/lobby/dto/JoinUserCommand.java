package io.jhpark.kopic.ge.lobby.dto;

public record JoinUserCommand(
	String userId,
	String nickname
) {
}
