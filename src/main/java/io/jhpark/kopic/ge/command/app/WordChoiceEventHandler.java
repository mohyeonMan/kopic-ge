package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.game.domain.DrawingPhase;
import io.jhpark.kopic.ge.game.domain.Game;
import io.jhpark.kopic.ge.game.domain.Turn;
import io.jhpark.kopic.ge.game.domain.TurnPhase;
import io.jhpark.kopic.ge.game.domain.TurnState;
import io.jhpark.kopic.ge.game.domain.WordChoicePhase;
import io.jhpark.kopic.ge.game.domain.CanvasState;
import io.jhpark.kopic.ge.room.app.RoomJobResult;
import io.jhpark.kopic.ge.room.app.RoomService;
import io.jhpark.kopic.ge.room.domain.Room;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WordChoiceEventHandler extends AbstractRoomEventHandler {

	public WordChoiceEventHandler(RoomService roomService) {
		super(roomService);
	}

	@Override
	public InboundRoomEventType supports() {
		return InboundRoomEventType.WORD_CHOICE;
	}

	@Override
	public EngineAck handle(RoomEventContext context) {
		if (context.payload() == null || !context.payload().has("choiceIndex")) {
			return rejectedAck(EngineAckReason.REJECTED);
		}

		int choiceIndex = context.payload().path("choiceIndex").asInt(-1);
		if (choiceIndex < 0) {
			return rejectedAck(EngineAckReason.REJECTED);
		}

		submit(context.roomId(), room -> {
			RoomJobResult result = applyExplicitWordChoice(
				room,
				context.userId(),
				choiceIndex,
				Instant.now()
			);
			log.info("word choice processed. roomId={}, userId={}, choiceIndex={}",
				context.roomId(), context.userId(), choiceIndex);
			return result;
		});
		return acceptedAck();
	}

	private RoomJobResult applyExplicitWordChoice(Room room, String userId, int choiceIndex, Instant now) {
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

		WordChoicePhase phase = requireWordChoicePhase(turn.getPhase());
		List<String> choices = phase.getWordChoices();
		if (choiceIndex < 0 || choiceIndex >= choices.size()) {
			return RoomJobResult.keep();
		}

		turn.setState(TurnState.DRAWING);
		turn.setEndReason(null);
		turn.setPhase(new DrawingPhase(
			choices.get(choiceIndex),
			new CanvasState(new ArrayList<>()),
			now,
			now.plusSeconds(game.settings().drawSec())
		));
		room.increaseVersion();
		return RoomJobResult.keep();
	}

	private WordChoicePhase requireWordChoicePhase(TurnPhase phase) {
		if (phase instanceof WordChoicePhase wordChoicePhase) {
			return wordChoicePhase;
		}
		throw new IllegalStateException("turn phase must be WordChoicePhase");
	}
}
