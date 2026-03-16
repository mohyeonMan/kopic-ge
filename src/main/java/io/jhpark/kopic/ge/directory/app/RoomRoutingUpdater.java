package io.jhpark.kopic.ge.directory.app;

public interface RoomRoutingUpdater {

	void putOwnerEngine(String roomId, String engineId);

	void removeOwnerEngine(String roomId);
}
