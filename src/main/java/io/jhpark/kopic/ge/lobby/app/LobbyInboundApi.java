package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.CreatePrivateRoomCommand;
import io.jhpark.kopic.ge.lobby.dto.CreateRandomRoomCommand;
import io.jhpark.kopic.ge.lobby.dto.JoinUserCommand;
import io.jhpark.kopic.ge.lobby.dto.PrivateRoomCreated;
import io.jhpark.kopic.ge.lobby.dto.QuickJoinResult;
import io.jhpark.kopic.ge.lobby.dto.RandomRoomCreated;

public interface LobbyInboundApi {

	PrivateRoomCreated createPrivateRoom(String engineId, CreatePrivateRoomCommand command);

	RandomRoomCreated createRandomRoom(String engineId, CreateRandomRoomCommand command);

	QuickJoinResult tryJoinRandomRoom(String engineId, String roomId, JoinUserCommand command);
}
