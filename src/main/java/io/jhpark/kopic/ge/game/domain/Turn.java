package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public record Turn(
	String turnId,
	String drawerUserId,
	String secretWord,
	List<String> wordChoices,
	TurnState state,
	Set<String> correctUserIds,
	TurnEndReason endReason,
	CanvasState canvas,
	Instant startedAt,
	Instant endsAt,
	Instant endedAt
) {
}
