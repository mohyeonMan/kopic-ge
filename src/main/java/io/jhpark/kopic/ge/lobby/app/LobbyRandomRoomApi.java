package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.CreateRandomRoomCommand;
import io.jhpark.kopic.ge.lobby.dto.RandomRoomCreated;

public interface LobbyRandomRoomApi {

	RandomRoomCreated createRandomRoom(String engineId, CreateRandomRoomCommand command);
}
