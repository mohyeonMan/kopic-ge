package io.jhpark.kopic.ge.directory.app;

public interface RoomCodeIndexUpdater {

	void putRoomCode(String roomCode, String roomId);

	void removeRoomCode(String roomCode);
}
