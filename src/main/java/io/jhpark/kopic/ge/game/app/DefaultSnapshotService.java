package io.jhpark.kopic.ge.game.app;

import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import org.springframework.stereotype.Service;

@Service
public class DefaultSnapshotService implements SnapshotService {

	@Override
	public ServerEnvelope buildSnapshot(String roomId, String requestId) {
		throw new UnsupportedOperationException("snapshot flow is pending domain redesign");
	}
}
