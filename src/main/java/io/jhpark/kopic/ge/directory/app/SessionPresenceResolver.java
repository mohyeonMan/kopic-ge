package io.jhpark.kopic.ge.directory.app;

import java.util.Collection;
import java.util.Map;

public interface SessionPresenceResolver {

	Map<String, String> resolveWsNodeIds(Collection<String> userIds);
}
