package io.jhpark.kopic.ge.game.app;

import org.springframework.stereotype.Service;

@Service
public class DefaultTurnOrchestrator implements TurnOrchestrator {

	@Override
	public void onTurnStarted(String roomId, String turnId) {
		// TODO: implement 3-second transition handling after previous turn end.
	}

	@Override
	public void onTurnTimeout(String roomId, String turnId) {
		// TODO: implement timeout-driven turn end flow.
	}
}
