package io.jhpark.kopic.ge.api.internal;

import io.jhpark.kopic.ge.lobby.dto.ReassignRoomResponse;
import org.springframework.stereotype.Component;

@Component
public class DefaultMigrationApi implements MigrationApi {

	@Override
	public ReassignRoomResponse reassignRoom(String roomId, String sourceEngineId) {
		return new ReassignRoomResponse(roomId, sourceEngineId, "target-stub", "http://ge-target:8080", true);
	}
}
