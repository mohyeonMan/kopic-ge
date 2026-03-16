package io.jhpark.kopic.ge.migration.domain;

import io.jhpark.kopic.ge.room.domain.GameSettings;
import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.RoomType;
import java.util.List;

public record MigrationSnapshot(
	String roomId,
	RoomType roomType,
	String roomCode,
	String hostUserId,
	int capacity,
	List<Participant> participants,
	GameSettings nextGameSettings,
	long roomVersion
) {
}
