package io.jhpark.kopic.ge.game.app;

import io.jhpark.kopic.ge.game.domain.Stroke;

public interface DrawCommandService {

	void drawStroke(String roomId, String userId, String turnId, Stroke stroke, String requestId);

	void clearCanvas(String roomId, String userId, String turnId, String requestId);
}
