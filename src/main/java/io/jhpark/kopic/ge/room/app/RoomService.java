package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.Room;
import java.util.Map;
import java.util.Optional;

public interface RoomService extends RoomClosingPort {

	Room createPrivateRoom(String engineId, String userId, String nickname, int capacity);

	Room createRandomRoom(String engineId, String userId, String nickname);

	Optional<Room> findRoom(String roomId);

	Map<String, Participant> getParticipants(String roomId);

	void submit(String roomId, RoomJob roomJob);
}
