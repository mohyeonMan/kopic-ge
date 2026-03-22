package io.jhpark.kopic.ge.outbound.app;

import io.jhpark.kopic.ge.directory.app.SessionPresenceResolver;
import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import io.jhpark.kopic.ge.outbound.dto.TargetedDelivery;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultBroadcastService implements BroadcastService {

	private final OutboundPublisher outboundPublisher;
	private final SessionPresenceResolver sessionPresenceResolver;

	public DefaultBroadcastService(
		OutboundPublisher outboundPublisher,
		SessionPresenceResolver sessionPresenceResolver
	) {
		this.outboundPublisher = outboundPublisher;
		this.sessionPresenceResolver = sessionPresenceResolver;
	}

	@Override
	public void toUser(String userId, ServerEnvelope envelope) {
		toUsers(List.of(userId), envelope);
	}

	@Override
	public void toUsers(Collection<String> userIds, ServerEnvelope envelope) {
		Map<String, String> wsNodeIdsByUserId = sessionPresenceResolver.resolveWsNodeIds(userIds);
		List<TargetedDelivery> deliveries = userIds.stream()
			.filter(wsNodeIdsByUserId::containsKey)
			.map(userId -> new TargetedDelivery(userId, wsNodeIdsByUserId.get(userId), envelope))
			.toList();
		if (!deliveries.isEmpty()) {
			outboundPublisher.publish(deliveries);
		}
		if (deliveries.size() != userIds.size()) {
			userIds.stream()
				.filter(userId -> !wsNodeIdsByUserId.containsKey(userId))
				.forEach(userId -> log.warn("session presence missing. userId={} eventCode={}", userId, envelope.e()));
		}
	}
}
