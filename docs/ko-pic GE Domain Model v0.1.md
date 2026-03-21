# ko-pic GE Domain Model v0.1

이 문서는 `Game Rules`를 안정적으로 구현하기 위한 MVP 기준 도메인 구조를 다시 정리한 초안이다.

핵심 원칙은 아래와 같다.

- `Room`은 방 자체와 "다음 게임을 시작하기 전까지의 준비 상태"를 가진다.
- `Game`은 실제로 시작된 "한 판"의 고정 규칙과 진행 상태를 가진다.
- `Round`와 `Turn`은 전체 히스토리 저장보다 "현재 진행 상태"를 정확히 표현하는 데 집중한다.
- 규칙 구현에 꼭 필요한 상태는 도메인 필드로 올리고, 아직 쓰지 않는 운영/확장 필드는 최대한 줄인다.

---

## 1. Room

방 자체를 표현하는 aggregate root다.

- `roomId`
  - 방의 고유 식별자.
- `roomCode`
  - private room 입장 코드.
  - random room이면 `null` 가능.
- `roomType`
  - `RANDOM | PRIVATE`
  - `public/private`가 아니라 `random/private`로 두는 이유:
  - `Game Rules`의 quick-join, auto-start, joinable index 정책은 `RANDOM` 여부가 핵심이기 때문이다.
- `participants: Map<UserId, Participant>`
  - 현재 방에 속한 참가자 목록.
- `state`
  - `LOBBY | RUNNING | MIGRATING | CLOSED`
- `createdAt`
  - 방 생성 시각.
- `hostUserId`
  - private room host.
  - random room이면 `null`.
- `settings: GameSettings`
  - 방이 현재 들고 있는 "다음 게임 시작 전 설정".
  - 대기실에서 수정 가능한 값이다.
  - 게임 시작 시 이 값을 복사해 `Game.settings`로 넘긴다.
- `currentGame: Game?`
  - 현재 이 방에서 진행 중이거나 결과 화면 중인 게임.
  - 게임이 없으면 `null`.
- `capacity`
  - 방 최대 인원.

### Room 주석

- `Room.settings`는 유지한다.
- 이유:
  - 게임 시작 전 대기실에서 설정을 바꿔야 한다.
  - 게임이 시작되면 그 시점의 설정을 복사해 현재 게임에만 적용해야 한다.
  - 즉 `Room.settings`와 `Game.settings`는 중복이 아니라 역할이 다르다.

- `Room`은 방 메타데이터와 현재 게임 핸들만 들도록 제한한다.
- 이유:
  - 점수, 현재 턴 정답자, 드로잉 phase 같은 "진행 중 게임 내부 상태"는 `Game` 이하로 내려가야 한다.
  - 그래야 room lifecycle과 game lifecycle이 섞이지 않는다.

---

## 2. Participant

방 참가자 정보다.

- `userId`
  - 사용자 고유 식별자.
- `name`
  - 닉네임.
- `status`
  - 참가자 현재 상태.
  - MVP에서는 실제 사용 범위가 좁을 수 있지만, 이후 연결 상태/준비 상태/추가 participant 상태 확장을 고려해 일단 유지한다.
- `wsNodeId`
  - 현재 이 참가자를 붙잡고 있는 WS 노드 식별값.
  - transport concern에 가깝지만, 현재 구조에서는 room membership과 함께 추적할 수 있게 일단 둔다.

### Participant 주석

- `status`, `wsNodeId`는 MVP 순수 규칙만 보면 과할 수 있다.
- 그래도 지금은 제거하지 않는다.
- 이유:
  - 네트워크/세션 계층으로 완전히 분리할지 아직 결정되지 않았고,
  - 운영상 참가자 추적에 필요할 가능성이 있어서 v0.1에서는 유지한다.

---

## 3. RoomState

- `LOBBY`
- `RUNNING`
- `MIGRATING`
- `CLOSED`

### RoomState 주석

- `RUNNING`이면 `currentGame != null`이어야 한다.
- `LOBBY`이면 `currentGame == null` 또는 다음 게임을 시작하기 전 상태여야 한다.
- `MIGRATING`에서는 새 게임 시작/입장 정책을 별도로 통제한다.

---

## 4. GameSettings

게임 설정 값 객체다.

- `roundCount`
- `drawSec`
- `wordChoiceSec`
- `wordChoiceCount`
- `endMode`

### GameSettings 주석

- `hintEnabled`, `hintInterval`은 v0.1에서 넣지 않는다.
- 이유:
  - 현재 `Game Rules`에 힌트 규칙이 없다.
  - 지금 넣으면 실제로 쓰이지 않는 필드가 도메인을 흐리게 만든다.

---

## 5. Game

실제로 시작된 "한 판"을 표현한다.

- `gameId`
  - 게임 고유 식별자.
- `roomId`
  - 이 게임이 속한 방 식별자.
- `status`
  - `RUNNING | RESULT_VIEW | ENDED`
- `settings: GameSettings`
  - 게임 시작 시 `Room.settings`를 복사한 스냅샷.
  - 이 게임 동안에는 절대 바뀌지 않는다.
- `scores: Map<UserId, Score>`
  - 현재 점수 현황.
- `currentRound: Round`
  - 현재 진행 중인 라운드.
- `startedAt`
  - 게임 시작 시각.
- `endedAt`
  - 게임 종료 확정 시각.
- `resultViewUntil`
  - 결과 화면을 유지할 종료 시각.
  - `Game Rules`의 "게임 종료 후 8초 유지"를 직접 표현하기 위해 둔다.

### Game 주석

- `READY` 상태는 제거한다.
- 이유:
  - 아직 시작하지 않은 상태는 `Room.currentGame == null`과 `Room.state == LOBBY`로 표현하는 편이 더 명확하다.

- `RESULT_VIEW`를 둔다.
- 이유:
  - `Game Rules`에는 게임 종료 직후 바로 lobby로 가지 않고 결과 화면 유지 시간이 있다.
  - 이 구간을 `ENDED` 하나로 뭉개면 규칙 표현력이 떨어진다.

- `List<Round>`는 두지 않는다.
- 이유:
  - v0.1에서는 전체 히스토리 저장보다 "현재 라운드/현재 턴을 정확히 표현하는 것"이 우선이다.
  - MVP 규칙 구현에 필요한 것은 전체 기록보다 현재 진행 상태와 전이 규칙이다.

---

## 6. Round

현재 라운드 진행 상태를 표현한다.

- `roundNo`
  - 현재 라운드 번호.
- `state`
  - `RUNNING | ENDED`
- `turnCursor`
  - 이번 라운드에서 현재 몇 번째 drawer 차례인지 나타내는 커서.
- `currentTurn: Turn`
  - 현재 턴 상태.
- `startedAt`
  - 라운드 시작 시각.
- `endedAt`
  - 라운드 종료 시각.

### Round 주석

- `roundId`는 v0.1에서 필수로 두지 않는다.
- 이유:
  - 현재 규칙 구현에서는 `gameId + roundNo`로도 충분히 식별 가능하다.

- `List<Turn>`도 두지 않는다.
- 이유:
  - 전체 턴 기록 저장보다 현재 턴 전이 구현이 더 중요하다.
  - 추후 replay나 audit가 필요하면 그때 별도 기록 모델로 분리하는 편이 낫다.

- `READY` 상태는 v0.1에서 제거한다.
- 이유:
  - 라운드 시작 전 대기 개념은 scheduler/transition으로 다루고,
  - 도메인 상태는 실제로 의미 있는 `RUNNING/ENDED` 중심으로 단순화한다.

---

## 7. Turn

현재 턴 상태를 표현한다.

- `turnId`
  - 턴 고유 식별자.
- `drawerUserId`
  - 현재 그리는 사람.
- `secretWord`
  - 정답 단어.
  - `WORD_CHOICE` 단계에서는 아직 확정 전일 수 있다.
- `wordChoices: List<String>`
  - drawer에게 제시된 후보 단어 목록.
- `state`
  - `WORD_CHOICE | DRAWING | ENDED`
- `correctUserIds: Set<UserId>`
  - 현재 턴에서 이미 정답 처리된 사용자 집합.
  - `Game Rules`의 중복 정답 방지 규칙을 직접 표현한다.
- `endReason`
  - `FIRST_CORRECT | ALL_CORRECT | TIMEOUT | DRAWER_LEFT`
  - 턴이 끝난 이유.
- `canvas: CanvasState`
  - 현재 턴 캔버스.
- `startedAt`
  - 현재 턴 단계 시작 시각.
- `endsAt`
  - 현재 턴 단계 종료 예정 시각.
- `endedAt`
  - 턴 종료 시각.

### Turn 주석

- `wordChoices`는 반드시 필요하다.
- 이유:
  - `Game Rules`의 `WORD_CHOICES -> WORD_CHOICE -> TURN_STARTED` 흐름을 직접 표현해야 하기 때문이다.

- `correctUserIds`는 반드시 필요하다.
- 이유:
  - 동일 턴에서 이미 정답 처리된 유저를 다시 점수 처리하지 않기 위한 핵심 상태다.

- `endReason`도 반드시 필요하다.
- 이유:
  - 턴 종료 후 다음 전환, 이벤트 발행, 점수 처리 맥락이 종료 사유에 따라 달라진다.

- `startedAt/endsAt`는 "현재 턴 상태의 deadline"을 나타내는 값으로 본다.
- 즉:
  - `WORD_CHOICE`면 단어 선택 제한시간
  - `DRAWING`이면 그림 그리기 제한시간

- 더 엄밀하게 가려면 `WordChoiceTurnState`, `DrawingTurnState`, `EndedTurnState`로 쪼갤 수 있다.
- 하지만 v0.1에서는 먼저 하나의 `Turn`에 필요한 필드를 모두 올려 MVP 규칙을 안정적으로 구현하는 데 집중한다.

---

## 8. CanvasState

현재 턴의 그림 상태다.

- `strokes: List<Stroke>`

### CanvasState 주석

- `Game Rules` 기준으로 서버는 "현재 턴"의 stroke만 유지하면 된다.
- 따라서 캔버스는 `Turn` 밑에 두는 게 맞다.

---

## 9. Stroke

드로잉 입력 값 객체다.

- `strokeId`
- `tool`
  - `PEN | ERASER`
- `colorIndex`
  - `0..19`
- `size`
  - `1..20`
- `points`
  - 각 포인트는 normalized coordinate를 사용한다.
  - 최대 64개.

### Stroke 주석

- 이 값 객체는 `Game Rules`의 드로잉 제한을 그대로 품는다.
- 즉 도메인 객체에서 최대한 빨리 invalid stroke를 막는 역할을 해야 한다.

---

## 10. 보강 불변식

v0.1에서 꼭 문서화해둘 불변식은 아래와 같다.

- `Room.state == RUNNING`이면 `Room.currentGame != null`
- `Room.roomType == RANDOM`이면 `hostUserId == null`
- `Room.roomType == PRIVATE`이면 `roomCode != null`
- 게임 시작 시 `Room.settings`를 복사해 `Game.settings`로 고정한다
- 게임 진행 중에는 반드시 `Game.settings`만 참조한다
- `Turn.state == WORD_CHOICE`이면 `wordChoices`가 비어 있으면 안 된다
- `Turn.state == DRAWING`이면 `secretWord`가 확정돼 있어야 한다
- `Turn.state == ENDED`이면 `endReason`과 `endedAt`이 있어야 한다
- `Turn.correctUserIds`에는 `drawerUserId`가 들어가면 안 된다

---

## 11. v0.1 요약

이 구조는 아래 선택을 명시적으로 한 버전이다.

- `Room.settings`와 `Game.settings`는 둘 다 유지
- participant의 `status`, `wsNodeId`도 일단 유지
- 전체 라운드/턴 히스토리보다 현재 진행 상태에 집중
- 결과 화면 유지 구간을 `Game.status`로 명시
- 턴 규칙 구현에 필요한 필드(`wordChoices`, `correctUserIds`, `endReason`)를 도메인에 직접 반영

즉 v0.1은 "최소한의 구조로 MVP 규칙을 안정적으로 구현하기 위한 현재 상태 중심 모델"이다.
