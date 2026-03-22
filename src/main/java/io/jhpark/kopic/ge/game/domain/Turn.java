package io.jhpark.kopic.ge.game.domain;

import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public final class Turn {

	private final String turnId;
	private final String drawerUserId;
	private TurnState state;
	private final Set<String> correctUserIds;
	private final Map<String, Integer> pendingScores;
	private TurnEndReason endReason;
	private TurnPhase phase;

}
