package io.jhpark.kopic.ge.config;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class GrpcServerConfig {

	@Bean
	public SmartLifecycle grpcServerLifecycle(
		List<BindableService> grpcServices,
		@Value("${grpc.server.port:9090}") int grpcPort
	) {
		return new SmartLifecycle() {
			private Server server;
			private volatile boolean running;

			@Override
			public void start() {
				try {
					ServerBuilder<?> builder = ServerBuilder.forPort(grpcPort);
					grpcServices.forEach(builder::addService);
					server = builder.build().start();
					running = true;
					log.info("grpc server started. port={}, services={}", grpcPort, grpcServices.size());
				} catch (IOException exception) {
					throw new IllegalStateException("failed to start grpc server", exception);
				}
			}

			@Override
			public void stop() {
				if (server != null) {
					server.shutdown();
				}
				running = false;
			}

			@Override
			public boolean isRunning() {
				return running;
			}

			@Override
			public int getPhase() {
				return Integer.MAX_VALUE;
			}

			@Override
			public boolean isAutoStartup() {
				return true;
			}

			@Override
			public void stop(Runnable callback) {
				stop();
				callback.run();
			}
		};
	}
}
