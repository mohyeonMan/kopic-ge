package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.ReassignRoomResponse;

public interface LobbyMigrationCoordinatorPort {

	ReassignRoomResponse requestReassign(String roomId, String sourceEngineId);
}
