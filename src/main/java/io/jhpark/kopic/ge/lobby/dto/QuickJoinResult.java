package io.jhpark.kopic.ge.lobby.dto;

import io.jhpark.kopic.ge.room.domain.Room;

public record QuickJoinResult(
	boolean joined,
	boolean created,
	String reason,
	Room room
) {
}
