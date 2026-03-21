package io.jhpark.kopic.ge.game.app;

import io.jhpark.kopic.ge.room.domain.GameSettings;
import org.springframework.stereotype.Service;

@Service
public class DefaultGameStartService implements GameStartService {

	@Override
	public void startGame(String roomId, String userId, GameSettings settings, String requestId) {
		throw new UnsupportedOperationException("game start flow is pending domain redesign");
	}
}
