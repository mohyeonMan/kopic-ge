package io.jhpark.kopic.ge.game.app;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jhpark.kopic.ge.game.domain.GameRuntime;
import io.jhpark.kopic.ge.game.domain.GameStatus;
import io.jhpark.kopic.ge.game.domain.ScoreBoard;
import io.jhpark.kopic.ge.game.domain.TurnRuntime;
import io.jhpark.kopic.ge.outbound.app.OutboundPublisher;
import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import io.jhpark.kopic.ge.outbound.dto.TargetedDelivery;
import io.jhpark.kopic.ge.room.app.RoomRegistry;
import io.jhpark.kopic.ge.room.domain.Room;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DefaultGuessCommandService implements GuessCommandService {

	private final RoomRegistry roomRegistry;
	private final OutboundPublisher outboundPublisher;

	public DefaultGuessCommandService(RoomRegistry roomRegistry, OutboundPublisher outboundPublisher) {
		this.roomRegistry = roomRegistry;
		this.outboundPublisher = outboundPublisher;
	}

	@Override
	public void submitGuess(String roomId, String userId, String text, String requestId) {
		Room room = roomRegistry.findByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));
		GameRuntime game = requireRunningGame(room);
		TurnRuntime turn = game.turnState();

		String normalized = normalize(text);
		boolean correct = normalize(turn.secretWord()).equals(normalized)
			&& !game.correctUsersInTurn().contains(userId)
			&& !turn.drawerUserId().equals(userId);

		if (!correct) {
			ObjectNode payload = JsonNodes.obj()
				.put("userId", userId)
				.put("name", room.participants().getOrDefault(userId, room.participants().values().iterator().next()).name())
				.put("text", text);
			outboundPublisher.publish(room.participants().values().stream()
				.map(participant -> new TargetedDelivery(participant.userId(), new ServerEnvelope(403, payload, requestId)))
				.toList());
			return;
		}

		Set<String> correctUsers = new LinkedHashSet<>(game.correctUsersInTurn());
		correctUsers.add(userId);
		Map<String, Integer> scores = new LinkedHashMap<>(game.scores().scores());
		scores.compute(userId, (k, v) -> v == null ? 1 : v + 1);
		scores.compute(turn.drawerUserId(), (k, v) -> v == null ? 1 : v + 1);

		GameRuntime updatedGame = new GameRuntime(
			game.gameId(),
			game.status(),
			game.settings(),
			game.round(),
			game.turn(),
			game.turnState(),
			new ScoreBoard(scores),
			correctUsers,
			game.resultViewUntil()
		);
		Room updatedRoom = new Room(
			room.roomId(), room.roomType(), room.roomCode(), room.ownerEngineId(), room.state(), room.participants(),
			room.hostUserId(), room.settings(), updatedGame, room.version() + 1, room.capacity()
		);
		roomRegistry.save(updatedRoom);

		ObjectNode correctPayload = JsonNodes.obj()
			.put("userId", userId)
			.put("name", room.participants().get(userId).name());
		ArrayNode scoreArray = JsonNodes.obj().putArray("scores");
		scores.forEach((scoreUserId, score) -> {
			ObjectNode scoreNode = scoreArray.addObject();
			scoreNode.put("userId", scoreUserId);
			scoreNode.put("score", score);
		});
		ObjectNode scorePayload = JsonNodes.obj();
		scorePayload.set("scores", scoreArray);

		outboundPublisher.publish(updatedRoom.participants().values().stream()
			.map(participant -> new TargetedDelivery(participant.userId(), new ServerEnvelope(404, correctPayload, requestId)))
			.toList());
		outboundPublisher.publish(updatedRoom.participants().values().stream()
			.map(participant -> new TargetedDelivery(participant.userId(), new ServerEnvelope(405, scorePayload, requestId)))
			.toList());
	}

	private GameRuntime requireRunningGame(Room room) {
		if (room.game() == null || room.game().status() != GameStatus.RUNNING) {
			throw new IllegalStateException("game not running");
		}
		return room.game();
	}

	private String normalize(String input) {
		String lowered = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFC)
			.toLowerCase()
			.replace(" ", "")
			.replace(".", "")
			.replace(",", "")
			.replace("!", "")
			.replace("?", "");
		return lowered;
	}
}
