package io.jhpark.kopic.ge.command.app;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultRoomEventHandlerRegistry implements RoomEventHandlerRegistry {

	private final Map<InboundRoomEventType, RoomEventHandler> handlers;

	public DefaultRoomEventHandlerRegistry(List<RoomEventHandler> handlers) {
		this.handlers = handlers.stream()
			.collect(Collectors.toUnmodifiableMap(RoomEventHandler::supports, Function.identity()));
	}

	@Override
	public RoomEventHandler get(InboundRoomEventType eventType) {
		RoomEventHandler handler = handlers.get(eventType);
		if (handler == null) {
			throw new IllegalArgumentException("no room event handler for " + eventType);
		}
		return handler;
	}
}
