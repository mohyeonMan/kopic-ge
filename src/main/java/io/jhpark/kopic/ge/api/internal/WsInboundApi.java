package io.jhpark.kopic.ge.api.internal;

import io.jhpark.kopic.ge.command.dto.EngineAck;
import io.jhpark.kopic.ge.command.dto.EngineEnvelopeRequest;
import io.jhpark.kopic.ge.command.dto.SessionLifecycleEvent;

public interface WsInboundApi {

	EngineAck handleEnvelope(EngineEnvelopeRequest request);

	EngineAck handleSessionLifecycle(SessionLifecycleEvent event);
}
