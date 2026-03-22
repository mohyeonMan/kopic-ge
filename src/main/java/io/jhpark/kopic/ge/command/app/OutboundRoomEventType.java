package io.jhpark.kopic.ge.command.app;

public enum OutboundRoomEventType {

	ROOM_STATE(301),
	GAME_STARTED(302),
	ROUND_STARTED(303),
	TURN_STARTED(304),
	TURN_ENDED(305),
	ROUND_ENDED(306),
	GAME_ENDED(307),
	CANVAS_STROKE(401),
	CANVAS_CLEAR(402),
	GUESS_MESSAGE(403),
	GUESS_CORRECT(404),
	SCORE_UPDATED(405),
	WORD_CHOICES(406),
	GAME_SNAPSHOT(408);

	private final int eventCode;

	OutboundRoomEventType(int eventCode) {
		this.eventCode = eventCode;
	}

	public int eventCode() {
		return eventCode;
	}
}
