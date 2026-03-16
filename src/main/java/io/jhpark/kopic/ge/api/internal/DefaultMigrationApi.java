package io.jhpark.kopic.ge.api.internal;

import io.jhpark.kopic.ge.lobby.app.LobbyMigrationCoordinatorPort;
import io.jhpark.kopic.ge.lobby.dto.ReassignRoomResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultMigrationApi implements MigrationApi {

	private final LobbyMigrationCoordinatorPort lobbyMigrationCoordinatorPort;

	public DefaultMigrationApi(LobbyMigrationCoordinatorPort lobbyMigrationCoordinatorPort) {
		this.lobbyMigrationCoordinatorPort = lobbyMigrationCoordinatorPort;
	}

	@Override
	public ReassignRoomResponse reassignRoom(String roomId, String sourceEngineId) {
		return lobbyMigrationCoordinatorPort.requestReassign(roomId, sourceEngineId);
	}
}
