package io.jhpark.kopic.ge.migration.app;

import io.jhpark.kopic.ge.lobby.dto.ReassignRoomResponse;
import io.jhpark.kopic.ge.migration.domain.MigrationResult;
import io.jhpark.kopic.ge.migration.domain.MigrationState;
import org.springframework.stereotype.Component;

@Component
public class DefaultMigrationSourceService implements MigrationSourceService {

	@Override
	public MigrationResult migrateWaitingRoom(String roomId, String sourceEngineId, String targetEngineId, String targetEngineEndpoint) {
		try {
			ReassignRoomResponse response = new ReassignRoomResponse(
				roomId,
				sourceEngineId,
				targetEngineId,
				targetEngineEndpoint,
				true
			);
			if (!response.reassigned()) {
				return new MigrationResult(false, MigrationState.FAILED, "reassign rejected");
			}
			return new MigrationResult(true, MigrationState.PREPARED, "target allocated");
		} catch (Exception exception) {
			return new MigrationResult(false, MigrationState.FAILED, exception.getMessage());
		}
	}
}
