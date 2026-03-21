package io.jhpark.kopic.ge.room.app;

import java.time.Duration;
import java.util.Objects;

public record RoomFollowUp(
	RoomJob job,
	Duration delay
) {

	public RoomFollowUp {
		Objects.requireNonNull(job, "job");
	}

	public static RoomFollowUp immediate(RoomJob job) {
		return new RoomFollowUp(job, Duration.ZERO);
	}

	public static RoomFollowUp delayed(Duration delay, RoomJob job) {
		return new RoomFollowUp(job, delay);
	}
}
