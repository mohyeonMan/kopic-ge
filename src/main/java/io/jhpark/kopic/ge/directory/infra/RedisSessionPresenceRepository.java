package io.jhpark.kopic.ge.directory.infra;

import io.jhpark.kopic.ge.directory.app.SessionPresenceRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisSessionPresenceRepository implements SessionPresenceRepository {

	private final StringRedisTemplate stringRedisTemplate;
	private final String redisKeyPrefix;

	public RedisSessionPresenceRepository(
		StringRedisTemplate stringRedisTemplate,
		@Value("${session-presence.redis-key-prefix:session:presence:user:}") String redisKeyPrefix
	) {
		this.stringRedisTemplate = stringRedisTemplate;
		this.redisKeyPrefix = redisKeyPrefix;
	}

	@Override
	public Optional<String> findWsNodeId(String userId) {
		return Optional.ofNullable(stringRedisTemplate.opsForValue().get(key(userId)));
	}

	@Override
	public Map<String, String> findWsNodeIds(Collection<String> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Map.of();
		}
		List<String> orderedKeys = userIds.stream()
			.map(this::key)
			.toList();
		List<String> values = stringRedisTemplate.opsForValue().multiGet(orderedKeys);
		Map<String, String> resolved = new LinkedHashMap<>();
		int index = 0;
		for (String userId : userIds) {
			String wsNodeId = values == null || values.size() <= index ? null : values.get(index);
			if (wsNodeId != null && !wsNodeId.isBlank()) {
				resolved.put(userId, wsNodeId);
			}
			index++;
		}
		return resolved;
	}

	@Override
	public void upsert(String userId, String wsNodeId) {
		stringRedisTemplate.opsForValue().set(key(userId), wsNodeId);
	}

	@Override
	public void remove(String userId) {
		stringRedisTemplate.delete(key(userId));
	}

	private String key(String userId) {
		return redisKeyPrefix + userId;
	}
}
