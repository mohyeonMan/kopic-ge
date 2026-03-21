package io.jhpark.kopic.ge.room.domain;

import io.jhpark.kopic.ge.game.domain.Game;
import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class Room {

	private final String roomId;
	private String roomCode;
	private final RoomType roomType;
	private final Map<String, Participant> participants;
	private RoomState state;
	private final Instant createdAt;
	private String hostUserId;
	private GameSettings settings;
	private Game currentGame;
	private final String ownerEngineId;
	private long version;
	private final int capacity;

	public void increaseVersion() {
		this.version += 1;
	}
}
