package io.jhpark.kopic.ge.directory.app;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface SessionPresenceRepository {

	Optional<String> findWsNodeId(String userId);

	Map<String, String> findWsNodeIds(Collection<String> userIds);

	void upsert(String userId, String wsNodeId);

	void remove(String userId);
}
