package io.jhpark.kopic.ge.game.domain;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public final class CanvasState {

	private final List<Stroke> strokes;
}
