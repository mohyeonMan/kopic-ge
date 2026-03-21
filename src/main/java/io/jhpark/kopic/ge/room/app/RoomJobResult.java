package io.jhpark.kopic.ge.room.app;

import java.util.List;

public record RoomJobResult(
	RoomJobOutcome outcome,
	List<RoomFollowUp> followUps
) {

	public RoomJobResult {
		followUps = followUps == null ? List.of() : List.copyOf(followUps);
	}

	public static RoomJobResult keep() {
		return new RoomJobResult(RoomJobOutcome.KEEP, List.of());
	}

	public static RoomJobResult keepWith(RoomFollowUp... followUps) {
		return new RoomJobResult(RoomJobOutcome.KEEP, List.of(followUps));
	}

	public static RoomJobResult deleteSlot() {
		return new RoomJobResult(RoomJobOutcome.DELETE_SLOT, List.of());
	}
}
