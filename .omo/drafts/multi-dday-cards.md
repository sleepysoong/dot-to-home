# Draft: Multi D-Day Cards on One Wallpaper

## Requirements (confirmed)
- [feature goal]: 한 배경화면에 여러 디데이 도트 카드를 배치하고 싶음 (예: 생일 디데이 + 시험 디데이)

## Technical Decisions
- [status]: 미결정 (현재 코드/패턴 조사 후 결정 예정)

## Research Findings
- [status]: explore 에이전트 2개 병렬 분석 진행 중

## Open Questions
- 카드 최대 개수 제한이 필요한지
- 카드 간 레이아웃 방식(자동 그리드/수동 위치)
- 잠금화면/홈화면 별 카드 세트 분리 여부
- 기존 단일 카드 설정 마이그레이션 방식
- 테스트 전략(TDD / 테스트 후 / 자동화 테스트 없음)

## Scope Boundaries
- INCLUDE: 멀티 카드 데이터 모델, UI 설정 흐름, 배경 생성 로직 확장
- EXCLUDE: 미정
