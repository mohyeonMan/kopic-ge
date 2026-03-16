package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.JoinUserCommand;
import io.jhpark.kopic.ge.lobby.dto.QuickJoinResult;
import io.jhpark.kopic.ge.room.app.RoomJoinService;
import org.springframework.stereotype.Component;

@Component
public class DefaultLobbyQuickJoinApi implements LobbyQuickJoinApi {

	private final RoomJoinService roomJoinService;

	public DefaultLobbyQuickJoinApi(RoomJoinService roomJoinService) {
		this.roomJoinService = roomJoinService;
	}

	@Override
	public QuickJoinResult tryJoinRandomRoom(String engineId, String roomId, JoinUserCommand command) {
		try {
			return new QuickJoinResult(
				true,
				false,
				null,
				roomJoinService.join(roomId, command.userId(), command.name())
			);
		} catch (IllegalStateException roomFull) {
			return new QuickJoinResult(false, false, "ROOM_FULL", null);
		} catch (IllegalArgumentException roomNotFound) {
			return new QuickJoinResult(false, false, "ROOM_NOT_FOUND", null);
		}
	}
}
