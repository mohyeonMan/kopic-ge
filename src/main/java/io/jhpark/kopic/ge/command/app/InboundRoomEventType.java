package io.jhpark.kopic.ge.command.app;

import io.jhpark.kopic.ge.command.dto.SessionLifecycleType;

public enum InboundRoomEventType {

	GAME_START_REQUEST(105),
	GAME_SNAPSHOT_REQUEST(106),
	DRAW_STROKE(201),
	DRAW_CLEAR(202),
	GUESS_SUBMIT(204),
	WORD_CHOICE(205),
	ROOM_JOIN(-1),
	ROOM_LEAVE(-2);

	private final int envelopeCode;

	InboundRoomEventType(int envelopeCode) {
		this.envelopeCode = envelopeCode;
	}

	public int envelopeCode() {
		return envelopeCode;
	}

	public static InboundRoomEventType fromClientEnvelopeCode(int eventCode) {
		return switch (eventCode) {
			case 105 -> GAME_START_REQUEST;
			case 106 -> GAME_SNAPSHOT_REQUEST;
			case 201 -> DRAW_STROKE;
			case 202 -> DRAW_CLEAR;
			case 204 -> GUESS_SUBMIT;
			case 205 -> WORD_CHOICE;
			default -> throw new IllegalArgumentException("unsupported client event code: " + eventCode);
		};
	}

	public static InboundRoomEventType fromSessionType(SessionLifecycleType sessionType) {
		return switch (sessionType) {
			case JOIN -> ROOM_JOIN;
			case LEAVE -> ROOM_LEAVE;
		};
	}
}
