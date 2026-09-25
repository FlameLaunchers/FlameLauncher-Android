package kr.co.donghyun.flamelauncher.data.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * 앱 표시 언어 — 시스템 언어(기본) · 영어 · 한국어.
 *
 * ⚠️ 안드로이드 13+ 는 시스템 설정에 앱별 언어가 있지만(res/xml/locales_config.xml),
 *    12 이하에는 없다. 그래서 고른 값을 여기 저장하고 액티비티가 붙기 전에
 *    [wrap] 으로 컨텍스트를 감싼다 — 두 버전 모두 앱 안 설정 하나로 바꾼다.
 */
enum class AppLocale(val tag: String?) {
    SYSTEM(null), ENGLISH("en"), KOREAN("ko");

    companion object {
        private const val PREFS = "app_locale"
        private const val KEY = "tag"

        fun load(context: Context): AppLocale {
            val tag = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            return entries.firstOrNull { it.tag == tag } ?: SYSTEM
        }

        fun save(context: Context, locale: AppLocale) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
                if (locale.tag == null) remove(KEY) else putString(KEY, locale.tag)
            }.apply()

            // 13+ 에서는 시스템 쪽 앱별 언어도 같은 값으로 맞춘다 — 설정 앱과 어긋나지 않게.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching {
                    context.getSystemService(LocaleManager::class.java)?.applicationLocales =
                        locale.tag?.let { LocaleList.forLanguageTags(it) } ?: LocaleList.getEmptyLocaleList()
                }
            }
        }

        /** 고른 언어로 컨텍스트를 감싼다. 시스템 언어면 그대로 돌려준다. */
        fun wrap(context: Context): Context {
            val locale = load(context).tag?.let(Locale::forLanguageTag) ?: return context
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            config.setLocales(LocaleList(locale))
            return context.createConfigurationContext(config)
        }
    }
}
