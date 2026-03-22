package io.jhpark.kopic.ge.directory.infra;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.jhpark.kopic.ge.directory.app.SessionPresenceRepository;
import io.jhpark.kopic.ge.directory.app.SessionPresenceResolver;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CachedSessionPresenceResolver implements SessionPresenceResolver {

	private final SessionPresenceRepository sessionPresenceRepository;
	private final Cache<String, Optional<String>> cache;

	public CachedSessionPresenceResolver(
		SessionPresenceRepository sessionPresenceRepository,
		@Value("${session-presence.cache-ttl-seconds:5}") long cacheTtlSeconds
	) {
		this.sessionPresenceRepository = sessionPresenceRepository;
		this.cache = Caffeine.newBuilder()
			.expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds))
			.maximumSize(10_000)
			.build();
	}

	@Override
	public Map<String, String> resolveWsNodeIds(Collection<String> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}
		Map<String, String> resolved = new LinkedHashMap<>();
		Map<String, Optional<String>> cachedValues = cache.getAllPresent(userIds);
		for (String userId : userIds) {
			Optional<String> cached = cachedValues.get(userId);
			if (cached != null) {
				cached.ifPresent(wsNodeId -> resolved.put(userId, wsNodeId));
			}
		}

		Collection<String> missingUserIds = userIds.stream()
			.filter(userId -> !cachedValues.containsKey(userId))
			.toList();
		if (!missingUserIds.isEmpty()) {
			Map<String, String> loaded = sessionPresenceRepository.findWsNodeIds(missingUserIds);
			for (String userId : missingUserIds) {
				String wsNodeId = loaded.get(userId);
				Optional<String> cached = Optional.ofNullable(wsNodeId);
				cache.put(userId, cached);
				cached.ifPresent(value -> resolved.put(userId, value));
			}
		}
		return resolved;
	}
}
