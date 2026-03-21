package io.jhpark.kopic.ge.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RoomRunnerConfig {

	@Bean("roomRunnerExecutor")
	public Executor roomRunnerExecutor(@Value("${room-runner.workers:4}") int workers) {
		return Executors.newFixedThreadPool(workers, Thread.ofPlatform().name("room-runner-", 0).factory());
	}

	@Bean("roomRunnerScheduler")
	public ScheduledExecutorService roomRunnerScheduler(@Value("${room-runner.scheduler-workers:2}") int workers) {
		return Executors.newScheduledThreadPool(workers, Thread.ofPlatform().name("room-scheduler-", 0).factory());
	}
}
