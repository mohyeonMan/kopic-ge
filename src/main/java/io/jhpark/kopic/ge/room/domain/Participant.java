package io.jhpark.kopic.ge.room.domain;

public record Participant(
	String userId,
	String nickname,
	ParticipantStatus status
) {
}
