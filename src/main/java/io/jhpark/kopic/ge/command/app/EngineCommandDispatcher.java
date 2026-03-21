package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineEnvelopeRequest;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleEvent;

public interface EngineCommandDispatcher {

	EngineAck handleEnvelope(EngineEnvelopeRequest request);

	EngineAck handleSessionLifecycle(SessionLifecycleEvent event);
}
