package io.jhpark.kopic.ge.migration.domain;

public record MigrationResult(
	boolean success,
	MigrationState state,
	String message
) {
}
