package io.jhpark.kopic.ge.common.error;

public enum ErrorCode {
	INVALID_EVENT(900),
	INVALID_PAYLOAD(901),
	ROOM_NOT_FOUND(902),
	ROOM_FULL(903),
	NOT_ROOM_HOST(904),
	GAME_ALREADY_STARTED(905),
	NOT_DRAWER(906),
	GAME_NOT_RUNNING(907),
	UNAUTHORIZED(908),
	INTERNAL_ERROR(909),
	RATE_LIMITED(910),
	NOT_OWNER(911),
	ROOM_MIGRATING(912);

	private final int protocolCode;

	ErrorCode(int protocolCode) {
		this.protocolCode = protocolCode;
	}

	public int protocolCode() {
		return protocolCode;
	}
}
