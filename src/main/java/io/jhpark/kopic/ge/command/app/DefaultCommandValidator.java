package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.ClientEnvelope;
import org.springframework.stereotype.Component;

@Component
public class DefaultCommandValidator implements CommandValidator {

	@Override
	public void validateEnvelope(ClientEnvelope envelope) {
		if (envelope == null) {
			throw new IllegalArgumentException("envelope is required");
		}
		if (envelope.e() <= 0) {
			throw new IllegalArgumentException("e must be positive");
		}
		if (envelope.p() == null || !envelope.p().isObject()) {
			throw new IllegalArgumentException("p must be an object");
		}
	}
}
