package io.jhpark.kopic.ge.room.domain;

public record GameSettings(
	int roundCount,
	int drawSec,
	int wordChoiceSec,
	int wordChoiceCount,
	DrawerOrderMode drawerOrderMode,
	EndMode endMode
) {

	public GameSettings {
		if (roundCount < 3 || roundCount > 10) {
			throw new IllegalArgumentException("roundCount must be 3..10");
		}
		if (drawSec < 20 || drawSec > 60) {
			throw new IllegalArgumentException("drawSec must be 20..60");
		}
		if (wordChoiceSec < 5 || wordChoiceSec > 15) {
			throw new IllegalArgumentException("wordChoiceSec must be 5..15");
		}
		if (wordChoiceCount < 3 || wordChoiceCount > 5) {
			throw new IllegalArgumentException("wordChoiceCount must be 3..5");
		}
		if (drawerOrderMode == null) {
			throw new IllegalArgumentException("drawerOrderMode must not be null");
		}
	}
}
