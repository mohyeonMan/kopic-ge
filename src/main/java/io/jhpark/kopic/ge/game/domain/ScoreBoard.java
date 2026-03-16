package io.jhpark.kopic.ge.game.domain;

import java.util.Map;

public record ScoreBoard(
	Map<String, Integer> scores
) {
}
