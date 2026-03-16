package io.jhpark.kopic.ge.room.domain;

import io.jhpark.kopic.ge.game.domain.GameRuntime;
import java.util.Map;

public record Room(
	String roomId,
	RoomType roomType,
	String roomCode,
	String ownerEngineId,
	RoomState state,
	Map<String, Participant> participants,
	String hostUserId,
	GameSettings settings,
	GameRuntime game,
	long version,
	int capacity
) {
}
