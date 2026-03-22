package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class WordChoicePhase implements TurnPhase {

	private final List<String> wordChoices;
	private Instant startedAt;
	private Instant endsAt;
}
