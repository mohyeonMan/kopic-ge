package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.ReassignRoomResponse;
import org.springframework.stereotype.Component;

@Component
public class StubLobbyMigrationCoordinatorPort implements LobbyMigrationCoordinatorPort {

	@Override
	public ReassignRoomResponse requestReassign(String roomId, String sourceEngineId) {
		return new ReassignRoomResponse(roomId, sourceEngineId, "target-stub", "http://ge-target:8080", true);
	}
}
