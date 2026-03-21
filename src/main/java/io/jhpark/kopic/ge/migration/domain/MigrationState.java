package io.jhpark.kopic.ge.migration.domain;

public enum MigrationState {
	PREPARING,
	PREPARED,
	COMMITTED,
	ROLLED_BACK,
	FAILED
}
