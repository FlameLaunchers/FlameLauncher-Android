package kr.co.donghyun.flamelauncher.presentation.util.mods

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sodium 본체를 못 알아보면 Podium 이 자동 설치되지 않고, Sodium 은 Pojav 계열 환경에서
 * 스스로 실행을 거부한다 — 즉 게임이 아예 안 켜진다. 조용히 깨지는 종류라 이름 규칙만 따로 본다.
 */
class ModFileNamesTest {

    /** installPodiumIfSodium 이 "본체" 로 인정하는 집합과 같아야 한다. */
    private val sodiumBodies = setOf("sodium", "sodium-fabric", "sodium-neoforge", "embeddium")

    @Test
    fun `모든 시대의 Sodium 배포 이름을 본체로 알아본다`() {
        val real = listOf(
            "sodium-fabric-0.9.2+mc26.3.jar",            // 요즘(26.x)
            "sodium-neoforge-0.9.3-alpha.1+mc26.3.jar",
            "sodium-fabric-0.5.13+mc1.20.1.jar",
            "sodium-fabric-mc1.20.1-0.5.0.jar",          // MC 버전이 중간에 오던 시절
            "sodium-fabric-mc1.16.3-0.1.0.jar",
            "sodium-fabric-mc1.17.1-0.3.4+build.13.jar",
            "embeddium-0.3.31+mc1.20.1.jar",
        )
        for (name in real) {
            assertTrue("본체로 못 알아봄: $name", ModFileNames.prefix(name).lowercase() in sodiumBodies)
        }
    }

    @Test
    fun `부가 모드는 본체로 오인하지 않는다`() {
        val addons = listOf(
            "sodium-extra-0.5.9+mc1.20.1.jar",
            "reeses-sodium-options-1.7.2+mc1.20.1-build.101.jar",
            "indium-1.0.34+mc1.21.jar",
            "particlerain-4.0.0-beta.10+1.20.1-fabric.jar",
        )
        for (name in addons) {
            assertFalse("본체로 오인: $name", ModFileNames.prefix(name).lowercase() in sodiumBodies)
        }
    }

    @Test
    fun `이름이 mc 로 시작하는 모드는 건드리지 않는다`() {
        assertEquals("mcw-doors", ModFileNames.prefix("mcw-doors-1.1.0-mc1.20.1.jar"))
    }
}
