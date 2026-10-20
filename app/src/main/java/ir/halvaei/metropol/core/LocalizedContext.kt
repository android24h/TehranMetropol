package ir.halvaei.metropol.core

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

/**
 * Services, receivers and widgets run with the application context, which does not follow the
 * per-app language on Android 12 and below. This applies it explicitly.
 */
fun Context.withAppLocale(): Context {
    val locale = AppCompatDelegate.getApplicationLocales()[0] ?: return this
    val config = Configuration(resources.configuration).apply { setLocale(locale) }
    return createConfigurationContext(config)
}

val Context.isPersianLocale: Boolean
    get() = resources.configuration.locales[0].language == "fa"
