package kr.co.donghyun.flamelauncher.presentation.util.mods

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * FlameShares 커뮤니티 탭(posts 테이블)에 게시글을 올리는 API.
 * FlameSharesApi(모드팩 업로드)와 같은 anon key 보안 원칙을 따른다 — 삽입만 가능,
 * 수정/삭제 불가.
 */
object FlameCommunityApi {
    private const val PROJECT_URL = "https://scpjrodowspnirzfkeep.supabase.co"
    private const val ANON_KEY = "sb_publishable_jUEzOZnlODeuCd8JmibB-A_AekXvvCu"

    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * @param environment 실행환경 정보(기기/렌더러/MC버전/로더/Java/힙 등) — 웹에서
     *   라벨과 함께 그리드로 표시된다. 키 이름은 웹의 labelMap 과 맞춰야 한다:
     *   device, android, cpu_abi, renderer, mc_version, loader, java, heap_mb
     */
    fun createPost(
        author: String,
        authorUuid: String?,
        avatarUrl: String,
        title: String,
        content: String?,
        logContent: String?,
        environment: Map<String, String>?,
    ): Boolean {
        return try {
            val payload = mutableMapOf<String, Any?>(
                "author" to author,
                "author_uuid" to authorUuid,
                "avatar_url" to avatarUrl,
                "title" to title,
                "content" to content,
                "log_content" to logContent,
                "environment" to environment,
            )
            val json = gson.toJson(payload)
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$PROJECT_URL/rest/v1/posts")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $ANON_KEY")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("FLAME_LAUNCHER", "FlameShares 게시글 등록 실패 (${response.code}): ${response.body?.string()}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("FLAME_LAUNCHER", "FlameShares 게시글 등록 예외: ${e.message}", e)
            false
        }
    }
}
