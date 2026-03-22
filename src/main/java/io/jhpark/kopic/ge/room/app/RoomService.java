package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Room;

public interface RoomService extends RoomClosingPort {

	Room createPrivateRoom(String engineId, String userId, String name, int capacity);

	Room createRandomRoom(String engineId, String userId, String name);

	void submit(String roomId, RoomJob roomJob);
}
