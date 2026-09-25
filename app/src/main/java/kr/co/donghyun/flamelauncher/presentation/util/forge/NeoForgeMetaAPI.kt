package kr.co.donghyun.flamelauncher.presentation.util.forge

import java.net.HttpURLConnection
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

/**
 * NeoForge maven 에서 사용 가능한 빌드를 가져온다.
 * 버전 형식: "<MC_MINOR>.<MC_PATCH>.<BUILD>"
 *   예) 20.4.237 → MC 1.20.4 / 21.0.169 → MC 1.21 / 21.1.43 → MC 1.21.1
 *
 * 1.20.1 NeoForge 는 net.neoforged:forge 의 별도 fork artifact 라
 * 현재 미지원 (이 클래스는 1.20.2+ 만 다룸).
 */
class NeoForgeMetaAPI {

    companion object {
        private const val MAVEN_METADATA_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
    }

    fun listLoaders(mcVersion: String): List<ForgeLoaderEntry> {
        val matching = fetchAllVersions().filter { neoforgeVersionToMc(it) == mcVersion }
        if (matching.isEmpty()) return emptyList()

        // beta 가 아닌 가장 최신을 recommended 로
        val recommended = matching.lastOrNull { !it.contains("-beta", ignoreCase = true) }
        val latest = matching.last()

        return matching.asReversed().map { v ->
            ForgeLoaderEntry(
                mcVersion = mcVersion,
                forgeVersion = v,
                recommended = v == recommended,
                latest = v == latest,
            )
        }
    }

    /**
     * 네오포지 버전 → 대응 MC 버전.
     *
     * 스키마가 두 가지다:
     *  - 세 조각 `21.1.251`      → `1.21.1` (마인크래프트가 1.x 이던 시절, 앞의 1. 은 생략돼 있다)
     *  - 네 조각 `26.3.0.22-beta` → `26.3`  (26 부터 MC 버전이 1.x 가 아니다)
     * 네 조각을 안 다루면 26.x 에서 목록이 통째로 비어 "네오포지 없음" 으로 보인다.
     */
    private fun neoforgeVersionToMc(v: String): String? {
        val base = v.substringBefore("-") // "21.1.43-beta" 같은 접미사 제거
        val parts = base.split(".")
        if (parts.size >= 4) {
            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            return "$major.$minor"
        }
        if (parts.size < 3) return null
        val minor = parts[0].toIntOrNull() ?: return null
        val patch = parts[1].toIntOrNull() ?: return null
        return if (patch == 0) "1.$minor" else "1.$minor.$patch"
    }

    private fun fetchAllVersions(): List<String> {
        val xml = httpGet(MAVEN_METADATA_URL)
        val doc = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xml.byteInputStream())
        val nodes = doc.getElementsByTagName("version")
        return (0 until nodes.length).mapNotNull { nodes.item(it).textContent }
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            requestMethod = "GET"
        }
        try {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}