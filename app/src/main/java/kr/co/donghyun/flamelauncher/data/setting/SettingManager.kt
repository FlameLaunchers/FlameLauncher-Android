package kr.co.donghyun.flamelauncher.data.setting

import android.content.Context
import com.google.gson.Gson
import kr.co.donghyun.flamelauncher.data.jvm.JvmSettings
import java.io.File

data class Setting(
    val neverShowCautionAgain : Boolean = false,
    // 사용자가 "이 버전 건너뛰기"를 누른 업데이트 태그(예: "v2.0.0"). 이 버전은 다시 안내하지 않음.
    val skippedUpdateVersion : String? = null
)

object SettingManager {
    private const val FILE_NAME = "setting.json"
    private val gson = Gson()

    fun load(context: Context): Setting {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (!file.exists()) return Setting()
            val settings = gson.fromJson(file.readText(), Setting::class.java)
                ?: Setting()

            settings
        } catch (_: Exception) {
            Setting()
        }
    }

    fun save(context: Context, settings: Setting) {
        try {
            File(context.filesDir, FILE_NAME).writeText(gson.toJson(settings))
        } catch (_: Exception) {}
    }
}