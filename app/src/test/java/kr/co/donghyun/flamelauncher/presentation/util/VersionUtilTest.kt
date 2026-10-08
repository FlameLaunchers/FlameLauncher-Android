package kr.co.donghyun.flamelauncher.presentation.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ⚠️ 이 테스트가 막는 것: 스냅샷 꼬리표가 붙은 버전("26.4-snapshot-3")을 "26.0" 으로
 *    읽어서 SDL·LWJGL 3.4 스택을 못 받던 버그. 그러면 게임이
 *    `NoClassDefFoundError: org/lwjgl/sdl/SDLPlatform` 으로 즉사한다.
 */
class VersionUtilTest {

    @Test
    fun `26_3 이상은 SDL 을 쓴다`() {
        assertTrue(usesSdl("26.3"))
        assertTrue(usesSdl("26.4"))
        assertTrue(usesSdl("27.1"))
    }

    @Test
    fun `스냅샷 꼬리표가 붙어도 숫자만 읽는다`() {
        assertTrue(usesSdl("26.4-snapshot-3"))
        assertTrue(usesSdl("26.3-pre1"))
        assertTrue(needsLwjgl34("26.4-snapshot-3"))
        assertTrue(needsLwjgl34("26.2-rc1"))
    }

    @Test
    fun `26_2 는 LWJGL 3_4 지만 아직 GLFW 다`() {
        assertTrue(needsLwjgl34("26.2"))
        assertFalse(usesSdl("26.2"))
    }

    @Test
    fun `1_21 이하는 둘 다 아니다`() {
        assertFalse(usesSdl("1.21.4"))
        assertFalse(needsLwjgl34("1.21.4"))
        assertFalse(usesSdl("1.12.2"))
    }

    @Test
    fun `주차 스냅샷은 연도로 가른다`() {
        assertTrue(usesSdl("26w14a"))
        assertFalse(usesSdl("25w46a"))
    }
}
