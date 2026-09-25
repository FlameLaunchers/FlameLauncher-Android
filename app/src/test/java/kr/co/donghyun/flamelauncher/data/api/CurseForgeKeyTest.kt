package kr.co.donghyun.flamelauncher.data.api

import kr.co.donghyun.flamelauncher.BuildConfig
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Test

/**
 * 빌드에 박힌 암호문이 실제로 풀리는지. 여기서 깨지면 CurseForge 탭이 통째로 죽는데,
 * 런타임엔 조용히 빈 키로 떨어지므로(=목록만 안 뜸) 원인을 찾기 어렵다.
 */
class CurseForgeKeyTest {

    @Test
    fun `빌드에 든 암호문이 CurseForge 키로 풀린다`() {
        // 키 없이도 빌드는 된다(시크릿 없는 포크 등) — 그 경우는 검사할 게 없다.
        assumeFalse("이 빌드에는 키가 없음", BuildConfig.CURSEFORGE_KEY_CIPHER.isEmpty())

        val key = CurseForgeKey.value
        assertTrue("복호화 실패(빈 값)", key.isNotEmpty())
        // CurseForge 키는 bcrypt 형태다: $2a$10$… 총 60자.
        assertTrue("CurseForge 키 형태가 아님: ${key.take(7)}…(${key.length}자)",
            Regex("""^\$2[aby]\$\d{2}\$.{53}$""").matches(key))
        assertNotEquals("암호문과 평문이 같다 = 암호화가 안 됐다",
            BuildConfig.CURSEFORGE_KEY_CIPHER, key)
    }

    @Test
    fun `평문 키가 BuildConfig 에 문자열로 남지 않는다`() {
        assumeFalse(BuildConfig.CURSEFORGE_KEY_CIPHER.isEmpty())
        val key = CurseForgeKey.value
        for (field in listOf(BuildConfig.CURSEFORGE_KEY_CIPHER,
                             BuildConfig.CURSEFORGE_KEY_IV,
                             BuildConfig.CURSEFORGE_KEY_PASS)) {
            assertTrue("BuildConfig 에 평문 키가 그대로 들어있다", !field.contains(key))
        }
    }
}
