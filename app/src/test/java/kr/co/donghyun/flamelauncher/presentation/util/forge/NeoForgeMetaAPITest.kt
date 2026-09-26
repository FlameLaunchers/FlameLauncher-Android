package kr.co.donghyun.flamelauncher.presentation.util.forge

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 네오포지 버전 → MC 버전 변환. 여기서 틀리면 해당 MC 버전의 빌드 목록이 통째로 비어
 * "네오포지 없음" 으로 보인다 — 26.3 에서 실제로 그랬다(네 조각 스키마를 안 다뤘다).
 */
class NeoForgeMetaAPITest {

    private val api = NeoForgeMetaAPI()

    @Test
    fun `네 조각 스키마는 MC 26 계열로 읽는다`() {
        // 실제 maven-metadata 에 있는 형태들
        assertEquals("26.3", api.neoforgeVersionToMc("26.3.0.22-beta"))
        assertEquals("26.3", api.neoforgeVersionToMc("26.3.0.0"))
        assertEquals("26.1", api.neoforgeVersionToMc("26.1.0.0-alpha.1+snapshot-1"))
    }

    @Test
    fun `세 조각 스키마는 예전처럼 1 점을 붙인다`() {
        assertEquals("1.21.1", api.neoforgeVersionToMc("21.1.251"))
        assertEquals("1.20.2", api.neoforgeVersionToMc("20.2.12-beta"))
        assertEquals("1.21", api.neoforgeVersionToMc("21.0.143"))   // patch 0 이면 1.21
    }

    @Test
    fun `모양이 다르면 무시한다`() {
        assertEquals(null, api.neoforgeVersionToMc("21.1"))
        assertEquals(null, api.neoforgeVersionToMc("garbage"))
    }
}
