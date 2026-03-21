package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.CreatePrivateRoomCommand;
import io.jhpark.kopic.ge.lobby.dto.PrivateRoomCreated;
import io.jhpark.kopic.ge.room.app.RoomLifecycleService;
import org.springframework.stereotype.Component;

@Component
public class DefaultLobbyPrivateRoomApi implements LobbyPrivateRoomApi {

	private final RoomLifecycleService roomLifecycleService;

	public DefaultLobbyPrivateRoomApi(RoomLifecycleService roomLifecycleService) {
		this.roomLifecycleService = roomLifecycleService;
	}

	@Override
	public PrivateRoomCreated createPrivateRoom(String engineId, CreatePrivateRoomCommand command) {
		return new PrivateRoomCreated(
			roomLifecycleService.createPrivateRoom(engineId, command.userId(), command.name(), command.capacity())
		);
	}
}
