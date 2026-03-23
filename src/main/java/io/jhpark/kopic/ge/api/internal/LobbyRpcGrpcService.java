package io.jhpark.kopic.ge.api.internal;

import io.grpc.stub.StreamObserver;
import io.jhpark.kopic.ge.lobby.dto.QuickJoinResult;
import io.jhpark.kopic.ge.room.app.RoomService;
import io.jhpark.kopic.ge.room.app.RoomSlotRepository;
import io.jhpark.kopic.ge.room.domain.Room;
import io.jhpark.kopic.ge.rpc.lobby.v1.CreatePrivateRoomRequestMessage;
import io.jhpark.kopic.ge.rpc.lobby.v1.CreateRandomRoomRequestMessage;
import io.jhpark.kopic.ge.rpc.lobby.v1.LobbyRpcServiceGrpc;
import io.jhpark.kopic.ge.rpc.lobby.v1.PrivateRoomCreatedMessage;
import io.jhpark.kopic.ge.rpc.lobby.v1.QuickJoinResultMessage;
import io.jhpark.kopic.ge.rpc.lobby.v1.RandomRoomCreatedMessage;
import io.jhpark.kopic.ge.rpc.lobby.v1.RoomSummaryMessage;
import io.jhpark.kopic.ge.rpc.lobby.v1.TryJoinRandomRoomRequestMessage;
import org.springframework.stereotype.Component;

@Component
public class LobbyRpcGrpcService extends LobbyRpcServiceGrpc.LobbyRpcServiceImplBase {

	private final RoomService roomService;
	private final RoomSlotRepository roomSlotRepository;

	public LobbyRpcGrpcService(
		RoomService roomService,
		RoomSlotRepository roomSlotRepository
	) {
		this.roomService = roomService;
		this.roomSlotRepository = roomSlotRepository;
	}

	@Override
	public void createPrivateRoom(
		CreatePrivateRoomRequestMessage request,
		StreamObserver<PrivateRoomCreatedMessage> responseObserver
	) {
		Room room = roomService.createPrivateRoom(
			request.getEngineId(),
			request.getUserId(),
			request.getNickname(),
			request.getCapacity()
		);
		responseObserver.onNext(PrivateRoomCreatedMessage.newBuilder().setRoom(toRoomSummary(room)).build());
		responseObserver.onCompleted();
	}

	@Override
	public void createRandomRoom(
		CreateRandomRoomRequestMessage request,
		StreamObserver<RandomRoomCreatedMessage> responseObserver
	) {
		Room room = roomService.createRandomRoom(
			request.getEngineId(),
			request.getUserId(),
			request.getNickname()
		);
		responseObserver.onNext(RandomRoomCreatedMessage.newBuilder().setRoom(toRoomSummary(room)).build());
		responseObserver.onCompleted();
	}

	@Override
	public void tryJoinRandomRoom(
		TryJoinRandomRoomRequestMessage request,
		StreamObserver<QuickJoinResultMessage> responseObserver
	) {
		QuickJoinResult result;
		try {
			Room room = roomSlotRepository.findRoomByRoomId(request.getRoomId())
				.orElseThrow(() -> new IllegalArgumentException("room not found"));
			if (!room.getParticipants().containsKey(request.getUserId()) && room.getParticipants().size() >= room.getCapacity()) {
				throw new IllegalStateException("room is full");
			}

			// This RPC only checks join availability.
			// Actual participant insertion is handled by WS lifecycle JOIN flow.
			result = new QuickJoinResult(true, false, null, room);
		} catch (IllegalStateException roomFull) {
			result = new QuickJoinResult(false, false, "ROOM_FULL", null);
		} catch (IllegalArgumentException roomNotFound) {
			result = new QuickJoinResult(false, false, "ROOM_NOT_FOUND", null);
		}

		QuickJoinResultMessage.Builder builder = QuickJoinResultMessage.newBuilder()
			.setJoined(result.joined())
			.setCreated(result.created());
		if (result.reason() != null) {
			builder.setReason(result.reason());
		}
		if (result.room() != null) {
			builder.setRoom(toRoomSummary(result.room()));
		}
		responseObserver.onNext(builder.build());
		responseObserver.onCompleted();
	}

	private RoomSummaryMessage toRoomSummary(Room room) {
		RoomSummaryMessage.Builder builder = RoomSummaryMessage.newBuilder()
			.setRoomId(room.getRoomId())
			.setRoomType(room.getRoomType().name())
			.setOwnerEngineId(room.getOwnerEngineId())
			.setCapacity(room.getCapacity())
			.setVersion(room.getVersion());
		if (room.getRoomCode() != null) {
			builder.setRoomCode(room.getRoomCode());
		}
		if (room.getHostUserId() != null) {
			builder.setHostUserId(room.getHostUserId());
		}
		return builder.build();
	}
}
