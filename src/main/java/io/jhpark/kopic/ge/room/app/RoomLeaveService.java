package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Room;

public interface RoomLeaveService {

	Room leave(String roomId, String userId);
}
