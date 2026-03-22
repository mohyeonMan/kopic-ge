package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineAckReason;
import io.jhpark.kopic.ge.room.app.RoomJobResult;
import io.jhpark.kopic.ge.room.app.RoomService;
import java.time.Instant;
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
			RoomJobResult result = WordChoiceFlowSupport.applyExplicitChoice(
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
}
