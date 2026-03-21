package io.jhpark.kopic.ge.outbound.app;

import io.jhpark.kopic.ge.outbound.dto.TargetedDelivery;
import io.jhpark.kopic.ge.outbound.dto.ServerEnvelope;
import io.jhpark.kopic.ge.room.domain.Room;
import java.util.List;

public interface AudienceResolver {

	List<TargetedDelivery> resolve(Room room, ServerEnvelope envelope);
}
