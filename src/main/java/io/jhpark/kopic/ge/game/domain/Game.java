package io.jhpark.kopic.ge.game.domain;

import io.jhpark.kopic.ge.room.domain.GameSettings;
import java.time.Instant;

public record Game(
	String gameId,
	String roomId,
	GameStatus status,
	GameSettings settings,
	ScoreBoard scores,
	Round currentRound,
	Instant startedAt,
	Instant endedAt,
	Instant resultViewUntil
) {
}
