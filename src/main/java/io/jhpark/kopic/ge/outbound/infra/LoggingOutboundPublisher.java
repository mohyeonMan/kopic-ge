package io.jhpark.kopic.ge.outbound.infra;

import io.jhpark.kopic.ge.outbound.app.OutboundPublisher;
import io.jhpark.kopic.ge.outbound.dto.TargetedDelivery;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingOutboundPublisher implements OutboundPublisher {

	private static final Logger log = LoggerFactory.getLogger(LoggingOutboundPublisher.class);

	@Override
	public void publish(List<TargetedDelivery> events) {
		events.forEach(event ->
			log.info("ge outbound userId={} wsNodeId={} eventCode={} requestId={}",
				event.userId(),
				event.wsNodeId(),
				event.envelope().e(),
				event.envelope().rid())
		);
	}
}
