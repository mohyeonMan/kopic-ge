package io.jhpark.kopic.ge.migration.app;

import io.jhpark.kopic.ge.lobby.app.LobbyMigrationCoordinatorPort;
import io.jhpark.kopic.ge.lobby.dto.ReassignRoomResponse;
import io.jhpark.kopic.ge.migration.domain.MigrationResult;
import io.jhpark.kopic.ge.migration.domain.MigrationState;
import org.springframework.stereotype.Component;

@Component
public class DefaultMigrationSourceService implements MigrationSourceService {

	private final LobbyMigrationCoordinatorPort lobbyMigrationCoordinatorPort;

	public DefaultMigrationSourceService(LobbyMigrationCoordinatorPort lobbyMigrationCoordinatorPort) {
		this.lobbyMigrationCoordinatorPort = lobbyMigrationCoordinatorPort;
	}

	@Override
	public MigrationResult migrateWaitingRoom(String roomId, String sourceEngineId, String targetEngineId, String targetEngineEndpoint) {
		try {
			ReassignRoomResponse response = lobbyMigrationCoordinatorPort.requestReassign(roomId, sourceEngineId);
			if (!response.reassigned()) {
				return new MigrationResult(false, MigrationState.FAILED, "reassign rejected");
			}
			return new MigrationResult(true, MigrationState.PREPARED, "target allocated");
		} catch (Exception exception) {
			return new MigrationResult(false, MigrationState.FAILED, exception.getMessage());
		}
	}
}
