package io.jhpark.kopic.ge.game.app;

import io.jhpark.kopic.ge.room.domain.GameSettings;

public interface GameStartService {

	void startGame(String roomId, String userId, GameSettings settings, String requestId);
}
