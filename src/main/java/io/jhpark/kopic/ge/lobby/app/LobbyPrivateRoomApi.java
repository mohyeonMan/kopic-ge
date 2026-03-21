package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.CreatePrivateRoomCommand;
import io.jhpark.kopic.ge.lobby.dto.PrivateRoomCreated;

public interface LobbyPrivateRoomApi {

	PrivateRoomCreated createPrivateRoom(String engineId, CreatePrivateRoomCommand command);
}
