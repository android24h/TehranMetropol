package ir.halvaei.metropol.data.repository

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import ir.halvaei.metropol.domain.model.AppLanguage
import ir.halvaei.metropol.domain.repository.LanguageRepository
import java.util.Locale

/**
 * Uses AndroidX per-app language support: the choice is persisted by AppCompat and the activity
 * is recreated with the new configuration (including RTL layout for Persian).
 */
class LanguageRepositoryImpl(context: Context) : LanguageRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun current(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: Locale.getDefault().language
        return if (tag == AppLanguage.PERSIAN.tag) AppLanguage.PERSIAN else AppLanguage.ENGLISH
    }

    override fun set(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    /**
     * Must run before the first activity is created (Application.onCreate) so the app opens in
     * the default language without a recreate. A flag, not an empty locale list, marks the first
     * launch: on Android 12 and below AppCompat only loads its stored choice when an activity
     * starts, so the list is always empty at this point.
     */
    override fun applyDefaultOnFirstLaunch() {
        if (prefs.getBoolean(KEY_DEFAULT_APPLIED, false)) return
        // Android 13+ keeps a choice made before this flag existed; respect it.
        if (AppCompatDelegate.getApplicationLocales().isEmpty) set(AppLanguage.DEFAULT)
        prefs.edit { putBoolean(KEY_DEFAULT_APPLIED, true) }
    }

    private companion object {
        const val PREFS_NAME = "language"
        const val KEY_DEFAULT_APPLIED = "default_applied"
    }
}
