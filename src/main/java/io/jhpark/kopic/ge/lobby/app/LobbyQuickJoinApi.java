package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.JoinUserCommand;
import io.jhpark.kopic.ge.lobby.dto.QuickJoinResult;

public interface LobbyQuickJoinApi {

	QuickJoinResult tryJoinRandomRoom(String engineId, String roomId, JoinUserCommand command);
}
