package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class Round {

	private final int roundNo;
	private RoundState state;
	private int turnCursor;
	private Turn currentTurn;
	private Instant startedAt;
	private Instant endedAt;

}
