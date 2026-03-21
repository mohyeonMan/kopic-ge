package io.jhpark.kopic.ge.common.error;

import io.jhpark.kopic.ge.command.dto.EngineAckReason;

public class EngineRejectedException extends RuntimeException {

	private final EngineAckReason reason;

	public EngineRejectedException(String message, EngineAckReason reason) {
		super(message);
		this.reason = reason;
	}

	public EngineAckReason reason() {
		return reason;
	}
}
