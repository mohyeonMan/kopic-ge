package io.jhpark.kopic.ge.room.domain;

public record Participant(
	String userId,
	String name,
	ParticipantStatus status,
	String wsNodeId
) {
}
