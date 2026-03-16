package io.jhpark.kopic.ge.api.internal;

import io.jhpark.kopic.ge.command.app.EngineCommandDispatcher;
import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineEnvelopeRequest;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleEvent;
import org.springframework.stereotype.Component;

@Component
public class DefaultEngineCommandApi implements EngineCommandApi {

	private final EngineCommandDispatcher engineCommandDispatcher;

	public DefaultEngineCommandApi(EngineCommandDispatcher engineCommandDispatcher) {
		this.engineCommandDispatcher = engineCommandDispatcher;
	}

	@Override
	public EngineAck handleEnvelope(EngineEnvelopeRequest request) {
		return engineCommandDispatcher.handleEnvelope(request);
	}

	@Override
	public EngineAck handleSessionLifecycle(SessionLifecycleEvent event) {
		return engineCommandDispatcher.handleSessionLifecycle(event);
	}
}
