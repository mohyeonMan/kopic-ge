package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class EndedPhase implements TurnPhase {

	private Instant endedAt;
}
