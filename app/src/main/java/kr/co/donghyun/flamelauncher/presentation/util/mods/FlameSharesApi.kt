package kr.co.donghyun.flamelauncher.presentation.util.mods

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder

/**
 * FlameShares(웹) 에 모드팩을 업로드하는 API — Supabase Storage(파일) + Table(메타데이터)
 * 두 단계로 이루어진다.
 *
 * ⚠️ 여기 쓰는 "anon key"는 GitHub 토큰과 성격이 다르다 — 애초에 클라이언트(앱)에
 * 그대로 노출해도 되게 설계된 공개 키이고, 실제 권한은 Supabase 프로젝트의 RLS
 * (Row Level Security) 정책으로 서버 쪽에서 제한한다. 이 프로젝트에는
 * "익명은 삽입(insert)만 가능, 조회는 가능, 수정/삭제는 불가"로 걸어뒀다 —
 * 즉 이 키가 그대로 유출돼도 기존 데이터를 망가뜨리거나 지울 수는 없다.
 */
object FlameSharesApi {
    private const val PROJECT_URL = "https://scpjrodowspnirzfkeep.supabase.co"
    private const val ANON_KEY = "sb_publishable_jUEzOZnlODeuCd8JmibB-A_AekXvvCu"
    private const val BUCKET = "modpacks"
    private const val TABLE = "modpacks"

    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * 모드팩 파일을 Storage 에 올리고, 메타데이터를 Table 에 등록한다.
     * @return 둘 다 성공하면 true.
     */
    fun upload(
        file: File,
        name: String,
        author: String,
        authorUuid: String?,
        avatarUrl: String,
        description: String?,
        mcVersion: String,
        loader: String,
        mods: List<Map<String, String>> = emptyList(),
    ): Boolean {
        val fileUrl = uploadFile(file) ?: run {
            Log.e("FLAME_LAUNCHER", "FlameShares: 파일 업로드 실패")
            return false
        }
        val ok = insertRow(
            name = name,
            author = author,
            authorUuid = authorUuid,
            avatarUrl = avatarUrl,
            description = description,
            mcVersion = mcVersion,
            loader = loader,
            fileUrl = fileUrl,
            sizeMb = file.length() / 1024.0 / 1024.0,
            mods = mods,
        )
        if (!ok) Log.e("FLAME_LAUNCHER", "FlameShares: 메타데이터 등록 실패")
        return ok
    }

    /** Storage 에 파일을 올리고, 공개 다운로드 URL을 돌려준다. */
    private fun uploadFile(file: File): String? {
        return try {
            // 같은 이름이 이미 있으면 충돌 나지 않도록 타임스탬프를 붙인다.
            val objectPath = "${System.currentTimeMillis()}_${file.name}"
            val encodedPath = objectPath.split("/").joinToString("/") {
                URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }
            val mediaType = "application/zip".toMediaType()
            val body = file.asRequestBody(mediaType)
            val request = Request.Builder()
                .url("$PROJECT_URL/storage/v1/object/$BUCKET/$encodedPath")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $ANON_KEY")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("FLAME_LAUNCHER", "FlameShares 업로드 실패 (${response.code}): ${response.body?.string()}")
                    return null
                }
                "$PROJECT_URL/storage/v1/object/public/$BUCKET/$encodedPath"
            }
        } catch (e: Exception) {
            Log.e("FLAME_LAUNCHER", "FlameShares 업로드 예외: ${e.message}", e)
            null
        }
    }

    /** modpacks 테이블에 메타데이터 한 줄을 등록한다. */
    private fun insertRow(
        name: String,
        author: String,
        authorUuid: String?,
        avatarUrl: String,
        description: String?,
        mcVersion: String,
        loader: String,
        fileUrl: String,
        sizeMb: Double,
        mods: List<Map<String, String>>,
    ): Boolean {
        return try {
            val json = gson.toJson(
                mapOf(
                    "name" to name,
                    "author" to author,
                    "author_uuid" to authorUuid,
                    "avatar_url" to avatarUrl,
                    "description" to description,
                    "mc_version" to mcVersion,
                    "loader" to loader,
                    "file_url" to fileUrl,
                    "size_mb" to sizeMb,
                    "mods" to mods,
                )
            )
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$PROJECT_URL/rest/v1/$TABLE")
                .header("apikey", ANON_KEY)
                .header("Authorization", "Bearer $ANON_KEY")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=minimal")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("FLAME_LAUNCHER", "FlameShares 메타데이터 실패 (${response.code}): ${response.body?.string()}")
                }
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e("FLAME_LAUNCHER", "FlameShares 메타데이터 예외: ${e.message}", e)
            false
        }
    }
}
