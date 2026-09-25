package kr.co.donghyun.flamelauncher.data.api

import android.util.Log
import kr.co.donghyun.flamelauncher.BuildConfig
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * CurseForge API 키. 빌드에는 암호문만 들어가고 여기서 풀어 쓴다.
 *
 * 열쇠는 `SHA-256(암호구절)`, 방식은 AES-256-CBC. 암호구절은 저장소에 없다
 * (`.secrets/curseforge.pass`, 없으면 빌드마다 무작위 생성).
 *
 * ⚠️ **난독화지 보안이 아니다.** 복호화에 필요한 게 전부 앱 안에 있으므로 바이너리를 뜯으면
 *    결국 나온다. 목적은 소스·저장소·`strings` 덤프에서 평문 키를 없애는 것뿐이다.
 *    키가 남용되면 CurseForge 콘솔에서 새로 발급하는 수밖에 없다.
 */
object CurseForgeKey {

    val value: String by lazy { decrypt() }

    val isConfigured: Boolean get() = value.isNotEmpty()

    private fun decrypt(): String {
        if (BuildConfig.CURSEFORGE_KEY_CIPHER.isEmpty()) return ""
        return try {
            val pass = Base64.getDecoder().decode(BuildConfig.CURSEFORGE_KEY_PASS)
            val aesKey = MessageDigest.getInstance("SHA-256").digest(pass)
            val iv = Base64.getDecoder().decode(BuildConfig.CURSEFORGE_KEY_IV)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
                init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(iv))
            }
            String(cipher.doFinal(Base64.getDecoder().decode(BuildConfig.CURSEFORGE_KEY_CIPHER)))
        } catch (e: Exception) {
            // 여기서 죽으면 CurseForge 탭 전체가 아니라 앱이 죽는다 — 조용히 빈 키로 떨어뜨린다.
            Log.w("FLAME_LAUNCHER", "CurseForge 키 복호화 실패: ${e.message}")
            ""
        }
    }
}
