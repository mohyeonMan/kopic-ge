package io.jhpark.kopic.ge.lobby.app;

import io.jhpark.kopic.ge.lobby.dto.JoinUserCommand;
import io.jhpark.kopic.ge.lobby.dto.QuickJoinResult;
import io.jhpark.kopic.ge.room.app.RoomSlotRepository;
import io.jhpark.kopic.ge.room.domain.Participant;
import io.jhpark.kopic.ge.room.domain.ParticipantStatus;
import io.jhpark.kopic.ge.room.domain.Room;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultLobbyQuickJoinApi implements LobbyQuickJoinApi {

	private final RoomSlotRepository roomSlotRepository;

	public DefaultLobbyQuickJoinApi(RoomSlotRepository roomSlotRepository) {
		this.roomSlotRepository = roomSlotRepository;
	}

	@Override
	public QuickJoinResult tryJoinRandomRoom(String engineId, String roomId, JoinUserCommand command) {
		try {
			Room room = roomSlotRepository.findRoomByRoomId(roomId)
				.orElseThrow(() -> new IllegalArgumentException("room not found"));
			if (!room.getParticipants().containsKey(command.userId()) && room.getParticipants().size() >= room.getCapacity()) {
				throw new IllegalStateException("room is full");
			}
			room.getParticipants().put(command.userId(),
				new Participant(command.userId(), command.name(), ParticipantStatus.ACTIVE, null));
			room.increaseVersion();
			log.info("quick-joined random room. roomId={}, userId={}, participantCount={}",
				roomId, command.userId(), room.getParticipants().size());
			return new QuickJoinResult(
				true,
				false,
				null,
				room
			);
		} catch (IllegalStateException roomFull) {
			return new QuickJoinResult(false, false, "ROOM_FULL", null);
		} catch (IllegalArgumentException roomNotFound) {
			return new QuickJoinResult(false, false, "ROOM_NOT_FOUND", null);
		}
	}
}
