package io.jhpark.kopic.ge.game.app;

public interface TurnOrchestrator {

	void onTurnStarted(String roomId, String turnId);

	void onTurnTimeout(String roomId, String turnId);
}
