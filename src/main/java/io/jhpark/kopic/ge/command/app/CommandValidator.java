package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.ClientEnvelope;

public interface CommandValidator {

	void validateEnvelope(ClientEnvelope envelope);
}
