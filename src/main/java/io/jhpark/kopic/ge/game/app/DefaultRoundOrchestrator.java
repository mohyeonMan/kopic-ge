package io.jhpark.kopic.ge.game.app;

import org.springframework.stereotype.Service;

@Service
public class DefaultRoundOrchestrator implements RoundOrchestrator {

	@Override
	public void onTurnEnded(String roomId) {
		// TODO: implement round boundary transitions and 4-second delay scheduling.
	}
}
