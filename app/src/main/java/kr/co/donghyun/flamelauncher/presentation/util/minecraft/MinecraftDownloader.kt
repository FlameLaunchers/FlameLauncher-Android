package kr.co.donghyun.flamelauncher.presentation.util.minecraft

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kr.co.donghyun.flamelauncher.data.mojang.DownloadPhase
import kr.co.donghyun.flamelauncher.data.mojang.DownloadProgress
import kr.co.donghyun.flamelauncher.data.mojang.MCPrepareResult
import kr.co.donghyun.flamelauncher.data.mojang.VersionEntry
import kr.co.donghyun.flamelauncher.data.mojang.VersionManifest
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.DigestInputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 바닐라 MC 다운로더 — 모든 파일을 instanceDir 하위에 저장
 *
 * instanceDir/
 *   libraries/
 *   versions/<versionId>/
 * 에셋(assets/indexes, assets/objects)은 [sharedAssetsDir] 이 있으면 인스턴스끼리 공유한다.
 */
class MinecraftDownloader(
    private val instanceDir: File,   // 인스턴스 루트 (예: instances/vanilla_1.21.4)
    private val versionEntry: VersionEntry,
    /**
     * 인스턴스가 함께 쓰는 에셋 폴더(보통 getExternalFilesDir(null)/assets).
     * 같은 MC 버전을 두 번 설치해도 오브젝트 수천 개를 다시 받지 않는다.
     * null 이면 예전처럼 인스턴스 안에 받는다.
     */
    private val sharedAssetsDir: File? = null,
    private val onProgress: (DownloadProgress) -> Unit
) {
    // 기본 OkHttpClient 는 호스트당 5개까지만 동시 요청한다. 에셋은 1~50KB 짜리가 수천 개라
    // 대역폭이 아니라 요청 왕복 횟수가 병목이어서, 동시 연결 수가 그대로 체감 속도가 된다.
    private val client = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 32
        })
        // ⚠️ 커넥션 풀 기본값은 유휴 5개다. 32개를 동시에 돌리면 끝난 커넥션이 곧바로 버려져서
        //    다음 파일마다 TLS 핸드셰이크를 다시 한다(모바일에선 파일당 100~200ms). 동시 수만큼 남긴다.
        .connectionPool(ConnectionPool(32, 5, TimeUnit.MINUTES))
        .build()
    private val gson = Gson()

    /**
     * 이미 인스턴스 안에 에셋을 받아둔 인스턴스는 그대로 둔다 — 실행할 때 런처가 인스턴스 폴더를
     * 먼저 보기 때문에, 인덱스는 인스턴스에 · 오브젝트는 공용에 나뉘면 텍스처가 통째로 빈다.
     */
    private val assetsDir: File = run {
        val local = File(instanceDir, "assets")
        val hasLocal = File(local, "indexes").listFiles()?.isNotEmpty() == true
        if (sharedAssetsDir == null || hasLocal) local else sharedAssetsDir
    }

    fun prepare(): MCPrepareResult {
        onProgress(DownloadProgress(phase = DownloadPhase.FETCHING_MANIFEST))
        val manifest = fetchManifest(versionEntry.url)

        // 클라이언트 JAR
        onProgress(DownloadProgress(phase = DownloadPhase.DOWNLOADING_CLIENT, fileName = "${manifest.id}.jar"))
        val clientJar = File(instanceDir, "versions/${manifest.id}/${manifest.id}.jar")
        downloadFile(manifest.downloads.client.url, clientJar, manifest.downloads.client.sha1,
            manifest.downloads.client.size)

        // 에셋 인덱스
        val assetIndexFile = File(assetsDir, "indexes/${manifest.assetIndex.id}.json")
        downloadFile(manifest.assetIndex.url, assetIndexFile, null)

        // 라이브러리
        val librariesDir = File(instanceDir, "libraries")
        val artifacts = manifest.libraries.mapNotNull { lib ->
            lib.downloads.artifact?.let { lib to it }
        }
        val done = AtomicInteger(0)
        runBlocking { forEachParallel(artifacts, LIBRARY_PARALLELISM) { (lib, artifact) ->
            // ⚠️ 경로는 매니페스트의 artifact.path 를 그대로 쓴다. 이름에서 다시 만들면 분류자가
            //    빠져서 "com.mojang:jtracy:1.14.38" 과 "…:natives-macos" 가 같은 파일이 된다.
            //    (순차로 받을 땐 본체가 먼저라 가려졌는데, 병렬로 받자 네이티브가 본체를 덮어
            //     TracyClient 클래스가 사라지고 26.3 이 Minecraft.<clinit> 에서 죽었다)
            val libFile = File(librariesDir, artifact.path ?: getLibraryPath(lib.name))
            downloadFile(artifact.url, libFile, artifact.sha1, artifact.size)
            onProgress(DownloadProgress(
                phase = DownloadPhase.DOWNLOADING_LIBRARIES,
                current = done.incrementAndGet(),
                total = artifacts.size,
                fileName = libFile.name
            ))
        } }

        // 에셋 오브젝트
        downloadAssets(assetIndexFile, File(assetsDir, "objects"))

        Log.d("FLAME_LAUNCHER", "✅ MC ${manifest.id} 준비 완료 → ${instanceDir.absolutePath} (에셋: ${assetsDir.absolutePath})")
        return MCPrepareResult(
            assetIndexId = manifest.assetIndex.id,
            mainClass = manifest.mainClass,
            minecraftArguments = manifest.minecraftArguments
        )
    }

    private fun fetchManifest(url: String): VersionManifest {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            val json = response.body?.string() ?: throw Exception("버전 JSON 읽기 실패")
            return gson.fromJson(json, VersionManifest::class.java)
        }
    }

    /**
     * 받는 동안엔 `.part` 로 두고 끝나면 이름을 바꾼다. 중간에 앱이 죽어도 반쪽짜리 파일이
     * 완성본처럼 남지 않는다(그러면 다음 실행 때 "있으니 건너뜀"으로 영영 안 고쳐진다).
     * sha1 이 오면 받으면서 같이 검사한다 — 어차피 스트림을 지나가므로 공짜다.
     */
    private fun downloadFile(url: String, destFile: File, expectedSha1: String?, expectedSize: Long = 0) {
        // 크기를 아는 경우엔 맞는지까지 본다 — 예전 버전이 분류자 충돌로 엉뚱한 jar 을 받아둔
        // 인스턴스가 있어서, 그냥 "있으면 통과" 로 두면 영영 안 고쳐진다.
        if (destFile.exists() && destFile.length() > 0 &&
            (expectedSize <= 0 || destFile.length() == expectedSize)
        ) return
        destFile.parentFile?.mkdirs()
        // .part 이름에 스레드를 섞는다. 같은 대상을 두 스레드가 받더라도 서로의 임시 파일을 안 밟는다.
        val part = File(destFile.parentFile, "${destFile.name}.${Thread.currentThread().id}.part")
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w("FLAME_LAUNCHER", "다운로드 실패 (${response.code}): $url")
                return
            }
            val body = response.body ?: return
            val digest = MessageDigest.getInstance("SHA-1")
            DigestInputStream(body.byteStream(), digest).use { input ->
                FileOutputStream(part).use { input.copyTo(it) }
            }
            if (expectedSha1 != null) {
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(expectedSha1, ignoreCase = true)) {
                    Log.w("FLAME_LAUNCHER", "SHA-1 불일치로 폐기: ${destFile.name}")
                    part.delete()
                    return
                }
            }
        }
        part.renameTo(destFile)
    }

    private fun downloadAssets(assetIndexFile: File, objectsDir: File) {
        if (!assetIndexFile.exists()) return
        val json = assetIndexFile.readText()
        val objects = com.google.gson.JsonParser.parseString(json)
            .asJsonObject["objects"].asJsonObject
        val hashes = objects.entrySet().map { it.value.asJsonObject["hash"].asString }
        val total = hashes.size
        val done = AtomicInteger(0)

        runBlocking { forEachParallel(hashes, ASSET_PARALLELISM) { hash ->
            val prefix = hash.substring(0, 2)
            try {
                // 에셋은 파일 이름이 곧 sha1 이라 검사도 공짜로 딸려온다.
                downloadFile(
                    "https://resources.download.minecraft.net/$prefix/$hash",
                    File(objectsDir, "$prefix/$hash"),
                    hash
                )
            } catch (_: Exception) {}
            val n = done.incrementAndGet()
            // 파일마다 UI 를 때리면 수천 번 리컴포지션이 돈다 — 32개마다 한 번만 보고.
            if (n % 32 == 0 || n == total) {
                onProgress(DownloadProgress(
                    phase = DownloadPhase.DOWNLOADING_ASSETS,
                    current = n,
                    total = total,
                    fileName = hash.take(12) + "..."
                ))
            }
        } }
    }

    /** 최대 [limit] 개씩 겹쳐서 돌린다. 모드팩 설치기(ModPackInstaller)와 같은 방식. */
    private suspend fun <T> forEachParallel(items: List<T>, limit: Int, body: (T) -> Unit) =
        coroutineScope {
            val semaphore = Semaphore(limit)
            items.map { item ->
                async(Dispatchers.IO) { semaphore.withPermit { body(item) } }
            }.awaitAll()
        }

    /** artifact.path 가 없을 때만 쓰는 폴백. 분류자(4번째 토큰)를 빠뜨리면 파일이 겹친다. */
    private fun getLibraryPath(name: String): String {
        val parts = name.split(":")
        val classifier = parts.getOrNull(3)?.takeIf { it.isNotBlank() }?.let { "-$it" } ?: ""
        return "${parts[0].replace('.', '/')}/${parts[1]}/${parts[2]}/" +
            "${parts[1]}-${parts[2]}$classifier.jar"
    }

    companion object {
        private const val ASSET_PARALLELISM = 32
        private const val LIBRARY_PARALLELISM = 8
    }
}
