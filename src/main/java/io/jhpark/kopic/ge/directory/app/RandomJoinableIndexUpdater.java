package io.jhpark.kopic.ge.directory.app;

public interface RandomJoinableIndexUpdater {

	void addJoinableRoom(String roomId, int score);

	void removeJoinableRoom(String roomId);
}
