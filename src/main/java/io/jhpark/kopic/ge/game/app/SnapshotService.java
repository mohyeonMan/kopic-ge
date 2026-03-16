package io.jhpark.kopic.ge.game.app;

import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;

public interface SnapshotService {

	ServerEnvelope buildSnapshot(String roomId, String requestId);
}
