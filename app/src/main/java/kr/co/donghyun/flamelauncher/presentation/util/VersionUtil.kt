package kr.co.donghyun.flamelauncher.presentation.util

/**
 * "26", "4-snapshot-3", "21" … 처럼 꼬리표가 붙을 수 있는 토큰에서 **앞쪽 숫자만** 읽는다.
 *
 * ⚠️ 예전에는 `toIntOrNull()` 로 통째로 읽었다. 그러면 "26.4-snapshot-3" 의 둘째 토큰이
 *    null → 0 이 되어 26.4 가 "26.0" 으로 취급됐고, SDL·LWJGL 3.4 스택을 못 받아
 *    3.3.6 으로 떴다. 실측: `NoClassDefFoundError: org/lwjgl/sdl/SDLPlatform` 으로 즉사.
 *    (iOS 는 문자열 대신 version.json 의 org.lwjgl:lwjgl 버전을 보기 때문에 멀쩡했다)
 */
private fun leadingInt(token: String?): Int =
    token?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 0

internal fun isVersionSupported(versionId: String): Boolean {
    val parts = versionId.split(".")
    parts.getOrNull(1)?.toIntOrNull() ?: 0
    parts.getOrNull(2)?.toIntOrNull() ?: 0
    return true
}

/**
 * 이 버전이 SDL3 로 창·입력을 만드는가 — 즉 26.3 이상인가.
 *
 * 26.3 의 version.json 에는 glfw 라이브러리가 **하나도 없고** org.lwjgl:lwjgl-sdl:3.4.3 이
 * 들어온다. 그래서 그 위로는 PojavLauncher 의 패치 GLFW 스택을 통째로 비켜가야 한다.
 * 25w.. 같은 스냅샷 표기는 연도 기준으로 26 년 것부터 SDL 로 본다.
 */
internal fun usesSdl(versionId: String): Boolean {
    val id = versionId.trim().lowercase()

    Regex("""^(\d{2})w\d+[a-z]$""").matchEntire(id)?.let {
        return it.groupValues[1].toInt() >= 26
    }

    val parts = id.split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: return false
    // 26 이전(1.21.x 등)은 전부 GLFW 다.
    if (major < 26) return false
    if (major > 26) return true
    return leadingInt(parts.getOrNull(1)) >= 3
}

/**
 * LWJGL 3.4 스택(= 3.4.3 네이티브)을 요구하는가. 26.2 부터다 — 26.2 는 아직 GLFW 지만
 * 3.4 콜백 인프라를 쓰고, 3.3.3 네이티브와 섞으면 클래스 초기화에서 죽는다(iOS 에서 실측).
 */
internal fun needsLwjgl34(versionId: String): Boolean {
    val id = versionId.trim().lowercase()

    Regex("""^(\d{2})w\d+[a-z]$""").matchEntire(id)?.let {
        return it.groupValues[1].toInt() >= 26
    }

    val parts = id.split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: return false
    if (major < 26) return false
    if (major > 26) return true
    return leadingInt(parts.getOrNull(1)) >= 2
}
