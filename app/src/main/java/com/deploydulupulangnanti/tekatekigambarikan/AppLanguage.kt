package com.deploydulupulangnanti.tekatekigambarikan

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

internal data class AppLanguage(val tag: String, val name: String)

internal val appLanguages = listOf(
    AppLanguage("en", "English"),
    AppLanguage("id", "Bahasa Indonesia"),
    AppLanguage("es", "Español"),
    AppLanguage("pt-BR", "Português (Brasil)"),
    AppLanguage("fr", "Français"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("ja", "日本語"),
    AppLanguage("ko", "한국어"),
    AppLanguage("zh-Hans", "简体中文"),
    AppLanguage("zh-Hant", "繁體中文"),
    AppLanguage("ar", "العربية"),
)

internal fun selectedLanguageTag(): String {
    val locale = AppCompatDelegate.getApplicationLocales()[0] ?: return ""
    val exact = appLanguages.firstOrNull { it.tag.equals(locale.toLanguageTag(), ignoreCase = true) }
    return exact?.tag ?: appLanguages.firstOrNull {
        LocaleListCompat.matchesLanguageAndScript(Locale.forLanguageTag(it.tag), locale)
    }?.tag.orEmpty()
}

internal fun selectLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}
