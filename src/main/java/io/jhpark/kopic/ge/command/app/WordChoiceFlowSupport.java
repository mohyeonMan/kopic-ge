package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.game.domain.Game;
import io.jhpark.kopic.ge.game.domain.Turn;
import io.jhpark.kopic.ge.game.domain.TurnState;
import io.jhpark.kopic.ge.room.app.RoomFollowUp;
import io.jhpark.kopic.ge.room.app.RoomJobResult;
import io.jhpark.kopic.ge.room.domain.Room;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class WordChoiceFlowSupport {

	private WordChoiceFlowSupport() {
	}

	static RoomFollowUp timeoutFollowUp(String expectedTurnId, long delaySeconds) {
		return RoomFollowUp.delayed(
			java.time.Duration.ofSeconds(delaySeconds),
			room -> applyAutoChoiceIfPending(room, expectedTurnId, Instant.now())
		);
	}

	static RoomJobResult applyExplicitChoice(Room room, String userId, int choiceIndex, Instant now) {
		Game game = room.getCurrentGame();
		if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
			return RoomJobResult.keep();
		}

		Turn turn = game.currentRound().getCurrentTurn();
		if (turn.getState() != TurnState.WORD_CHOICE) {
			return RoomJobResult.keep();
		}
		if (!turn.getDrawerUserId().equals(userId)) {
			return RoomJobResult.keep();
		}

		List<String> choices = turn.getWordChoices();
		if (choiceIndex < 0 || choiceIndex >= choices.size()) {
			return RoomJobResult.keep();
		}

		applyDrawingPhase(room, choices.get(choiceIndex), now);
		return RoomJobResult.keep();
	}

	static RoomJobResult applyAutoChoiceIfPending(Room room, String expectedTurnId, Instant now) {
		Game game = room.getCurrentGame();
		if (game == null || game.currentRound() == null || game.currentRound().getCurrentTurn() == null) {
			return RoomJobResult.keep();
		}

		Turn turn = game.currentRound().getCurrentTurn();
		if (!turn.getTurnId().equals(expectedTurnId) || turn.getState() != TurnState.WORD_CHOICE) {
			return RoomJobResult.keep();
		}

		List<String> choices = turn.getWordChoices();
		if (choices.isEmpty()) {
			return RoomJobResult.keep();
		}

		int choiceIndex = ThreadLocalRandom.current().nextInt(choices.size());
		applyDrawingPhase(room, choices.get(choiceIndex), now);
		return RoomJobResult.keep();
	}

	private static void applyDrawingPhase(Room room, String secretWord, Instant now) {
		Game game = room.getCurrentGame();
		Turn turn = game.currentRound().getCurrentTurn();

		turn.setSecretWord(secretWord);
		turn.setState(TurnState.DRAWING);
		turn.setStartedAt(now);
		turn.setEndsAt(now.plusSeconds(game.settings().drawSec()));
		turn.setEndedAt(null);
		turn.setEndReason(null);
		room.increaseVersion();
	}
}
