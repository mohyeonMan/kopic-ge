package io.jhpark.kopic.ge.game.app;

import io.jhpark.kopic.ge.game.domain.Stroke;
import org.springframework.stereotype.Service;

@Service
public class DefaultDrawCommandService implements DrawCommandService {

	@Override
	public void drawStroke(String roomId, String userId, String turnId, Stroke stroke, String requestId) {
		throw new UnsupportedOperationException("draw flow is pending domain redesign");
	}

	@Override
	public void clearCanvas(String roomId, String userId, String turnId, String requestId) {
		throw new UnsupportedOperationException("draw flow is pending domain redesign");
	}
}
