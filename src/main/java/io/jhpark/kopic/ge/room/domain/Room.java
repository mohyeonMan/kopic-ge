package io.jhpark.kopic.ge.room.domain;

import io.jhpark.kopic.ge.game.domain.Game;
import java.time.Instant;
import java.util.Map;

public record Room(
	String roomId,
	String roomCode,
	RoomType roomType,
	Map<String, Participant> participants,
	RoomState state,
	Instant createdAt,
	String hostUserId,
	GameSettings settings,
	Game currentGame,
	String ownerEngineId,
	long version,
	int capacity
) {
}
