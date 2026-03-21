package io.jhpark.kopic.ge.room.app;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultRoomRunner implements RoomRunner {

	private final RoomSlotRepository roomSlotRepository;
	private final Executor roomRunnerExecutor;
	private final ScheduledExecutorService roomRunnerScheduler;

	public DefaultRoomRunner(
		RoomSlotRepository roomSlotRepository,
		@Qualifier("roomRunnerExecutor") Executor roomRunnerExecutor,
		@Qualifier("roomRunnerScheduler") ScheduledExecutorService roomRunnerScheduler
	) {
		this.roomSlotRepository = roomSlotRepository;
		this.roomRunnerExecutor = roomRunnerExecutor;
		this.roomRunnerScheduler = roomRunnerScheduler;
	}

	@Override
	public void submit(String roomId, RoomJob job) {
		Objects.requireNonNull(roomId, "roomId");
		Objects.requireNonNull(job, "job");

		RoomSlot slot = roomSlotRepository.findSlotByRoomId(roomId)
			.orElseThrow(() -> new IllegalArgumentException("room not found"));

		slot.enqueue(() -> runJob(slot, job));
		log.debug("room job enqueued. roomId={}, hasPendingJobs={}", roomId, slot.hasPendingJobs());
		schedule(slot);
	}

	private void runJob(RoomSlot slot, RoomJob job) {
		try {
			RoomJobResult result = job.run(slot.room());
			if (result == null) {
				throw new IllegalStateException("room job result must not be null");
			}
			if (result.outcome() == RoomJobOutcome.DELETE_SLOT) {
				log.info("deleting room slot after job. roomId={}", slot.roomId());
				roomSlotRepository.deleteSlot(slot.roomId());
				return;
			}
			slot.touch(Instant.now());
			for (RoomFollowUp followUp : result.followUps()) {
				scheduleFollowUp(slot.roomId(), followUp);
			}
		} catch (Throwable throwable) {
			log.error("room job failed. roomId={}", slot.roomId(), throwable);
		}
	}

	private void scheduleFollowUp(String roomId, RoomFollowUp followUp) {
		long delayMillis = followUp.delay() == null ? 0L : Math.max(0L, followUp.delay().toMillis());
		if (delayMillis == 0L) {
			log.debug("submitting immediate follow-up. roomId={}", roomId);
			submit(roomId, followUp.job());
			return;
		}
		log.debug("scheduling delayed follow-up. roomId={}, delayMillis={}", roomId, delayMillis);
		roomRunnerScheduler.schedule(
			() -> submit(roomId, followUp.job()),
			delayMillis,
			TimeUnit.MILLISECONDS
		);
	}

	private void schedule(RoomSlot slot) {
		if (slot.markRunning()) {
			log.debug("scheduling room slot drain. roomId={}", slot.roomId());
			roomRunnerExecutor.execute(() -> drain(slot));
		}
	}

	private void drain(RoomSlot slot) {
		log.debug("room slot drain started. roomId={}", slot.roomId());
		try {
			Runnable job;
			while ((job = slot.poll()) != null) {
				job.run();
			}
		} finally {
			slot.finishRun();
			log.debug("room slot drain finished. roomId={}, hasPendingJobs={}", slot.roomId(), slot.hasPendingJobs());
			if (slot.hasPendingJobs()) {
				schedule(slot);
			}
		}
	}
}
