package kr.co.donghyun.flamelauncher.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// ── 푸른 불꽃 팔레트 ─────────────────────────────────────────────────────────
// 불꽃은 뜨거울수록 푸르다 — 심지 쪽이 시안, 바깥이 짙은 청색이다.
// 그 온도 그라데이션을 그대로 역할에 대응시킨다:
//   FlameAccent(가장 뜨거움) → FlamePrimary → FlameLight → FlameDark(바깥 불꽃)
// 배경은 검정이 아니라 **푸른기 도는 검정**이라 불꽃색이 겉돌지 않는다.
//
// ⚠️ 값은 iOS 의 FlameColor 와 1:1 로 맞춘다 — 두 플랫폼이 같은 색으로 보여야 한다.

val Orange = Color(0xFF5AB8FF)     // (이름은 유지) 중간 톤 블루
val BgDim = Color(0x9905080F)
val Flame = Color(0xFF2E9BFF)      // 포인트 컬러 (블루 플레임)
val BgDark = Color(0xFF05080F)     // 가장 어두운 배경 (푸른 검정)
val BgSurface = Color(0xFF0B1422)  // 카드 / 서피스 배경
val BgBorder = Color(0xFF1C2E47)   // 테두리
val TextMain = Color(0xFFE8F2FF)   // 기본 텍스트 (쿨 화이트)
val TextSub = Color(0xFF8CA2C0)    // 보조 텍스트 (쿨 그레이)
val BgItem   = Color(0xFF08101B)   // 리스트 아이템
val Green    = Color(0xFF3DD68C)   // 성공
val Red      = Color(0xFFFF5F7E)   // 오류
val WarnBg = Color(0xFF2A1424)     // 경고 배경

internal val FlamePrimary   = Color(0xFF2E9BFF)   // 메인 플레임 (핫 블루)
internal val FlameLight     = Color(0xFF7CC9FF)   // 라이트 플레임
internal val FlameDark      = Color(0xFF0B4A8F)   // 다크 플레임 (딥 블루)
internal val FlameAccent    = Color(0xFF22E0FF)   // 액센트 (시안 — 불꽃 심지)
internal val TextPrimary    = Color(0xFFE8F2FF)   // 쿨 화이트
internal val TextSecondary  = Color(0xFF8CA2C0)   // 쿨 그레이
internal val TagRelease     = Color(0xFF7CC9FF)   // 릴리즈 뱃지
internal val TagSnapshot    = Color(0xFFA78BFA)   // 스냅샷 뱃지 (바이올렛)
internal val TagOld         = Color(0xFF6D7F96)   // 구버전 뱃지
