package io.jhpark.kopic.ge.migration.app;

public interface MigrationOwnerPointerUpdater {

	boolean compareAndSetOwner(String roomId, String expectedSourceEngineId, String targetEngineId);
}
