package io.jhpark.kopic.ge.room.domain;

public record GameSettings(
	int roundCount,
	int drawSec,
	int wordChoiceSec,
	int wordChoiceCount,
	EndMode endMode
) {

	public static final int FIXED_WORD_CHOICE_SEC = 10;

	public GameSettings {
		if (roundCount < 3 || roundCount > 10) {
			throw new IllegalArgumentException("roundCount must be 3..10");
		}
		if (drawSec < 20 || drawSec > 60) {
			throw new IllegalArgumentException("drawSec must be 20..60");
		}
		if (wordChoiceSec != FIXED_WORD_CHOICE_SEC) {
			throw new IllegalArgumentException("wordChoiceSec must be 10");
		}
		if (wordChoiceCount < 3 || wordChoiceCount > 5) {
			throw new IllegalArgumentException("wordChoiceCount must be 3..5");
		}
	}
}
