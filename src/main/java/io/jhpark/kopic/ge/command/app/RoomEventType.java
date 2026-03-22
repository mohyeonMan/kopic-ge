package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.SessionLifecycleType;

public enum RoomEventType {

	GAME_START_REQUEST(105),
	GAME_SNAPSHOT_REQUEST(106),
	DRAW_STROKE(201),
	DRAW_CLEAR(202),
	GUESS_SUBMIT(204),
	WORD_CHOICE(205),
	ROOM_JOIN(-1),
	ROOM_LEAVE(-2);

	private final int envelopeCode;

	RoomEventType(int envelopeCode) {
		this.envelopeCode = envelopeCode;
	}

	public int envelopeCode() {
		return envelopeCode;
	}

	public static RoomEventType fromEnvelopeCode(int eventCode) {
		for (RoomEventType value : values()) {
			if (value.envelopeCode == eventCode) {
				return value;
			}
		}
		throw new IllegalArgumentException("unsupported event code: " + eventCode);
	}

	public static RoomEventType fromSessionType(SessionLifecycleType sessionType) {
		return switch (sessionType) {
			case JOIN -> ROOM_JOIN;
			case LEAVE -> ROOM_LEAVE;
		};
	}
}
