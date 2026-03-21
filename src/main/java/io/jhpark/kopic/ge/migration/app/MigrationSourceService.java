package io.jhpark.kopic.ge.migration.app;

import io.jhpark.kopic.ge.migration.domain.MigrationResult;

public interface MigrationSourceService {

	MigrationResult migrateWaitingRoom(String roomId, String sourceEngineId, String targetEngineId, String targetEngineEndpoint);
}
