package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;

public record TurnRuntime(
	String turnId,
	String drawerUserId,
	String secretWord,
	Instant startedAt,
	Instant endsAt,
	CanvasState canvas,
	TurnPhase phase
) {
}
