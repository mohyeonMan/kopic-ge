package io.jhpark.kopic.ge.api.internal;

import io.jhpark.kopic.ge.lobby.dto.ReassignRoomResponse;

public interface MigrationApi {

	ReassignRoomResponse reassignRoom(String roomId, String sourceEngineId);
}
