package kr.co.donghyun.flamelauncher.presentation.util.mods

/**
 * 모드 jar 파일 이름에서 "어떤 모드인가"만 뽑아내는 규칙.
 *
 * 같은 모드가 배포처·시대마다 다른 이름으로 온다:
 *   sodium-fabric-0.9.2+mc26.3.jar        (요즘)
 *   sodium-fabric-mc1.20.1-0.5.0.jar      (0.5.3 이전 — MC 버전이 이름 **중간**에 온다)
 * 뒤쪽 형태를 못 걸러내면 prefix 가 "sodium-fabric-mc1.20.1" 이 돼서 Sodium 본체로 인식되지
 * 않고, 그 결과 Podium 자동 설치가 조용히 건너뛰어져 게임이 아예 안 켜진다(실측 30개 파일).
 */
object ModFileNames {

    /** 이름 중간에 끼어든 MC 버전 토큰: `-mc1.20.1`, `_MC1.16.5` 등. `+mc1.21.1` 은 대상이 아니다. */
    private val MID_NAME_MC_VERSION = Regex("[-_]mc\\d[\\w.]*", RegexOption.IGNORE_CASE)

    /** 이름 맨 앞부터 "첫 숫자 직전"까지 = 모드 식별자. */
    private val LEADING_NAME = Regex("^([a-zA-Z][a-zA-Z0-9_\\-]*?)[-_]+\\d")

    /**
     * "sodium-fabric-0.9.2+mc26.3.jar"   → "sodium-fabric"
     * "sodium-fabric-mc1.20.1-0.5.0.jar" → "sodium-fabric"
     * "reeses-sodium-options-1.7.2.jar"  → "reeses-sodium-options" (부가 모드는 본체와 구분된다)
     */
    fun prefix(fileName: String): String {
        val nameOnly = fileName.removeSuffix(".jar").replace(MID_NAME_MC_VERSION, "")
        return LEADING_NAME.find(nameOnly)?.groupValues?.get(1) ?: nameOnly
    }
}
