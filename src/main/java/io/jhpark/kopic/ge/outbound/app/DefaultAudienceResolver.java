package io.jhpark.kopic.ge.outbound.app;

import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import io.jhpark.kopic.ge.outbound.dto.TargetedDelivery;
import io.jhpark.kopic.ge.room.domain.Room;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class DefaultAudienceResolver implements AudienceResolver {

	@Override
	public List<TargetedDelivery> resolve(Room room, ServerEnvelope envelope) {
		return room.participants().values().stream()
			.map(participant -> new TargetedDelivery(participant.userId(), envelope))
			.collect(Collectors.toList());
	}
}
