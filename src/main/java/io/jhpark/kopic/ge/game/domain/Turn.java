package io.jhpark.kopic.ge.game.domain;

import java.time.Instant;
import java.util.List;
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
	private String secretWord;
	private final List<String> wordChoices;
	private TurnState state;
	private final Set<String> correctUserIds;
	private TurnEndReason endReason;
	private final CanvasState canvas;
	private Instant startedAt;
	private Instant endsAt;
	private Instant endedAt;

}
