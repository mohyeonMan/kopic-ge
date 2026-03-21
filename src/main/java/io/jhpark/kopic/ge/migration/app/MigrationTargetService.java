package io.jhpark.kopic.ge.migration.app;

import io.jhpark.kopic.ge.migration.domain.MigrationResult;
import io.jhpark.kopic.ge.migration.domain.MigrationSnapshot;

public interface MigrationTargetService {

	MigrationResult prepareImport(MigrationSnapshot snapshot);
}
