package com.example.tehranmetropoll.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.tehranmetropoll.domain.model.AppLanguage
import com.example.tehranmetropoll.domain.repository.LanguageRepository
import java.util.Locale

/**
 * Uses AndroidX per-app language support: the choice is persisted by AppCompat and the activity
 * is recreated with the new configuration (including RTL layout for Persian).
 */
class LanguageRepositoryImpl : LanguageRepository {

    override fun current(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: Locale.getDefault().language
        return if (tag == AppLanguage.PERSIAN.tag) AppLanguage.PERSIAN else AppLanguage.ENGLISH
    }

    override fun set(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
