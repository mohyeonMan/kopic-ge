package io.jhpark.kopic.ge.migration.app;

import io.jhpark.kopic.ge.migration.domain.MigrationResult;
import io.jhpark.kopic.ge.migration.domain.MigrationSnapshot;
import io.jhpark.kopic.ge.migration.domain.MigrationState;
import org.springframework.stereotype.Component;

@Component
public class DefaultMigrationTargetService implements MigrationTargetService {

	@Override
	public MigrationResult prepareImport(MigrationSnapshot snapshot) {
		if (snapshot == null) {
			return new MigrationResult(false, MigrationState.FAILED, "snapshot is null");
		}
		return new MigrationResult(true, MigrationState.PREPARED, "prepared");
	}
}
