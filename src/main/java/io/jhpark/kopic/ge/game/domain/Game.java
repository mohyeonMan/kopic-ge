package io.jhpark.kopic.ge.game.domain;

import io.jhpark.kopic.ge.room.domain.GameSettings;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@AllArgsConstructor
@Accessors(fluent = true)
public final class Game {

	private final String gameId;
	private final String roomId;
	private GameStatus status;
	private final GameSettings settings;
	private final ScoreBoard scores;
	private Round currentRound;
	private Instant startedAt;
	private Instant endedAt;
	private Instant resultViewUntil;

}
