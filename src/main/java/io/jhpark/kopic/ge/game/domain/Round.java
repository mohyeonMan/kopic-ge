package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;

public record Round(
	int roundNo,
	RoundState state,
	int turnCursor,
	Turn currentTurn,
	Instant startedAt,
	Instant endedAt
) {
}
