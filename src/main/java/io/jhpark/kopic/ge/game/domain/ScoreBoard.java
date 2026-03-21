package io.jhpark.kopic.ge.game.domain;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@AllArgsConstructor
@Accessors(fluent = true)
public final class ScoreBoard {

	private final Map<String, Integer> scores;
}
