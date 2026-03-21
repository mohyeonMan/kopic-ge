package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.CreateRandomRoomCommand;
import io.jhpark.kopic.ge.lobby.dto.RandomRoomCreated;
import io.jhpark.kopic.ge.room.app.RoomLifecycleService;
import org.springframework.stereotype.Component;

@Component
public class DefaultLobbyRandomRoomApi implements LobbyRandomRoomApi {

	private final RoomLifecycleService roomLifecycleService;

	public DefaultLobbyRandomRoomApi(RoomLifecycleService roomLifecycleService) {
		this.roomLifecycleService = roomLifecycleService;
	}

	@Override
	public RandomRoomCreated createRandomRoom(String engineId, CreateRandomRoomCommand command) {
		return new RandomRoomCreated(
			roomLifecycleService.createRandomRoom(engineId, command.userId(), command.name())
		);
	}
}
