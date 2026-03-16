package io.jhpark.kopic.ge.game.app;

public interface GuessCommandService {

	void submitGuess(String roomId, String userId, String text, String requestId);
}
