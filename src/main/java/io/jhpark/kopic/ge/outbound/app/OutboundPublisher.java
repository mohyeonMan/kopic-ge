package io.jhpark.kopic.ge.outbound.app;

import io.jhpark.kopic.ge.outbound.dto.TargetedDelivery;
import java.util.List;

public interface OutboundPublisher {

	void publish(List<TargetedDelivery> events);
}
