package kr.co.donghyun.flamelauncher.data.mojang

import com.google.gson.annotations.SerializedName

data class VersionManifest(
    val id: String,
    val mainClass: String,
    val downloads: Downloads,
    val libraries: List<Library>,
    val assetIndex: AssetIndex,
    val minecraftArguments: String? = null   // ← 1.12 이전 (1.13+은 arguments 객체)
)

data class Downloads(
    val client: DownloadItem
)

data class Library(
    val name: String,
    val downloads: LibraryDownloads,
    val natives: Map<String, String>? = null // OS별 네이티브 파일 식별자
)

data class LibraryDownloads(
    val artifact: DownloadItem?,
    val classifiers: Map<String, DownloadItem>? = null
)

data class AssetIndex(
    val id: String,    // "1.16"
    val sha1: String,
    val size: Long,
    val totalSize: Long,
    val url: String    // "https://piston-meta.mojang.com/v1/packages/..."
)


data class DownloadItem(
    val url: String,
    val size: Long,
    val sha1: String,
    /**
     * 저장소 기준 상대 경로. 라이브러리 항목에만 있고, **이게 정본이다.**
     * 이름(group:artifact:version:classifier)에서 다시 만들면 분류자가 빠져
     * jtracy 처럼 항목 5개가 같은 파일로 겹친다(본체 jar 이 macOS 네이티브로 덮여 클래스 소실).
     */
    val path: String? = null
)

data class DownloadProgress(
    val phase: DownloadPhase = DownloadPhase.IDLE,
    val current: Int = 0,
    val total: Int = 0,
    val fileName: String = "",
    val error: String? = null
) {
    val fraction: Float get() = if (total > 0) current.toFloat() / total else 0f
    val percent: Int get() = (fraction * 100).toInt()
}

data class MCPrepareResult(
    val assetIndexId: String,
    val mainClass: String,
    val minecraftArguments: String?
)

enum class DownloadPhase {
    IDLE,
    FETCHING_MANIFEST,
    DOWNLOADING_CLIENT,
    DOWNLOADING_LIBRARIES,
    DOWNLOADING_ASSETS,
    DONE,
    ERROR
}


data class VersionManifestIndex(
    val latest: Latest,
    val versions: List<VersionEntry>
)

data class Latest(
    val release: String,
    val snapshot: String
)

data class VersionEntry(
    val id: String,
    val type: String, // "release", "snapshot", "old_beta", "old_alpha"
    val url: String,
    @SerializedName("releaseTime") val releaseTime: String
)
