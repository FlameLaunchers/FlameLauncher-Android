package kr.co.donghyun.flamelauncher.presentation.util.mods

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Instant

/**
 * FlameShares 웹에서 발급한 로그인 코드를, 이 앱의 진짜(정식 클라이언트 ID로 인증된)
 * Minecraft 세션 정보로 인증 완료 처리하는 API.
 *
 * RLS 정책상 login_codes 의 UPDATE 는 verified_at 이 아직 null 인 행에만 허용되므로
 * (Supabase 쪽에 이미 설정됨), 이미 사용된 코드를 재사용하는 요청은 서버에서
 * 자동으로 거부된다(응답 자체는 200이지만 실제로 변경된 행이 0개).
 */
object FlameCommunityAuthApi {
    private const val PROJECT_URL = "https://scpjrodowspnirzfkeep.supabase.co"
    private const val ANON_KEY = "sb_publishable_jUEzOZnlODeuCd8JmibB-A_AekXvvCu"

    private val client = OkHttpClient()
    private val gson = Gson()

    /** @return 인증 성공(실제로 코드 하나를 검증 완료 처리함) 여부. */
    fun verifyLoginCode(code: String, username: String, uuid: String): Boolean {
        return try {
            val avatarUrl = "https://crafatar.com/avatars/$uuid?size=64&overlay"
            val payload = mapOf(
                "verified_username" to username,
                "verified_uuid" to uuid,
                "verified_avatar_url" to avatarUrl,
                "verified_at" to Instant.now().toString(),
            )
            val json = gson.toJson(payload)
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$PROJECT_URL/rest/v1/login_codes?code=eq.$code")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $ANON_KEY")
                .header("Content-Type", "application/json")
                // Prefer: return=representation 로 실제 변경된 행을 돌려받아서,
                //   "코드가 없거나 이미 인증된 경우"(행 0개)를 정확히 구분한다.
                .header("Prefer", "return=representation")
                .patch(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("FLAME_LAUNCHER", "로그인 코드 인증 실패 (${response.code}): ${response.body?.string()}")
                    return false
                }
                val responseBody = response.body?.string() ?: "[]"
                val updatedRows = gson.fromJson(responseBody, Array<Any>::class.java)
                updatedRows.isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e("FLAME_LAUNCHER", "로그인 코드 인증 예외: ${e.message}", e)
            false
        }
    }
}
