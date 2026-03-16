package io.jhpark.kopic.ge.game.domain;

import io.jhpark.kopic.ge.room.domain.GameSettings;
import java.time.Instant;
import java.util.Set;

public record GameRuntime(
	String gameId,
	GameStatus status,
	GameSettings settings,
	RoundRuntime round,
	int turn,
	TurnRuntime turnState,
	ScoreBoard scores,
	Set<String> correctUsersInTurn,
	Instant resultViewUntil
) {
}
