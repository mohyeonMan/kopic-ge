package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Room;

public interface RoomJoinService {

	Room join(String roomId, String userId, String name);
}
