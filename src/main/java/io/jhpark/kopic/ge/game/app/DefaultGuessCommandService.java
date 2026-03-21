package io.jhpark.kopic.ge.game.app;

import org.springframework.stereotype.Service;

@Service
public class DefaultGuessCommandService implements GuessCommandService {

	@Override
	public void submitGuess(String roomId, String userId, String text, String requestId) {
		throw new UnsupportedOperationException("guess flow is pending domain redesign");
	}
}
