package io.jhpark.kopic.ge.migration.app;

import org.springframework.stereotype.Component;

@Component
public class DefaultMigrationOwnerPointerUpdater implements MigrationOwnerPointerUpdater {

	@Override
	public boolean compareAndSetOwner(String roomId, String expectedSourceEngineId, String targetEngineId) {
		// TODO: implement Redis CAS update in infra layer.
		return false;
	}
}
