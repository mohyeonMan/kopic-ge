package io.jhpark.kopic.ge.outbound.app;

import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import java.util.Collection;

public interface BroadcastService {

	void toUser(String userId, ServerEnvelope envelope);

	void toUsers(Collection<String> userIds, ServerEnvelope envelope);
}
