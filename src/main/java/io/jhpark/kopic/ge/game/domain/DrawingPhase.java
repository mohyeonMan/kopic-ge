package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class DrawingPhase implements TurnPhase {

	private final String secretWord;
	private final CanvasState canvas;
	private Instant startedAt;
	private Instant endsAt;
}
