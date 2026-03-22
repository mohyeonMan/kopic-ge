package io.jhpark.kopic.ge.game.domain;

public sealed interface TurnPhase permits WordChoicePhase, DrawingPhase, EndedPhase {
}
