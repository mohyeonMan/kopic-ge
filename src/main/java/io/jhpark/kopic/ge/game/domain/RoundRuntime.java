package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;

public record RoundRuntime(
	int roundNo,
	RoundState state,
	Instant startedAt,
	Instant endedAt,
	int turnCursor
) {
}
