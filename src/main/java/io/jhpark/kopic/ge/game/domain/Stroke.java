package io.jhpark.kopic.ge.game.domain;

import java.util.List;

public record Stroke(
	String strokeId,
	StrokeTool tool,
	int colorIndex,
	int size,
	List<Point> points
) {

	public Stroke {
		if (colorIndex < 0 || colorIndex > 19) {
			throw new IllegalArgumentException("colorIndex must be 0..19");
		}
		if (size < 1 || size > 20) {
			throw new IllegalArgumentException("size must be 1..20");
		}
		if (points == null || points.isEmpty() || points.size() > 64) {
			throw new IllegalArgumentException("points size must be 1..64");
		}
	}

	public record Point(double x, double y) {

		public Point {
			if (x < 0.0 || x > 1.0 || y < 0.0 || y > 1.0) {
				throw new IllegalArgumentException("point must be normalized 0.0..1.0");
			}
		}
	}
}
