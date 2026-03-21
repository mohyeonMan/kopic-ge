package io.jhpark.kopic.ge.room.app;

import io.jhpark.kopic.ge.room.domain.Room;
import java.time.Instant;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RoomSlot {

	private final String roomId;
	private final Queue<Runnable> mailbox = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean running = new AtomicBoolean(false);
	private volatile Room room;
	private volatile Instant lastTouchedAt;

	public RoomSlot(Room room) {
		this.room = Objects.requireNonNull(room, "room");
		this.roomId = room.getRoomId();
		this.lastTouchedAt = Instant.now();
	}

	public String roomId() {
		return roomId;
	}

	public Room room() {
		return room;
	}

	public Instant lastTouchedAt() {
		return lastTouchedAt;
	}

	public void touch(Instant touchedAt) {
		this.lastTouchedAt = Objects.requireNonNull(touchedAt, "touchedAt");
	}

	public void enqueue(Runnable job) {
		mailbox.add(Objects.requireNonNull(job, "job"));
	}

	public Runnable poll() {
		return mailbox.poll();
	}

	public boolean hasPendingJobs() {
		return !mailbox.isEmpty();
	}

	public boolean markRunning() {
		return running.compareAndSet(false, true);
	}

	public void finishRun() {
		running.set(false);
	}
}
