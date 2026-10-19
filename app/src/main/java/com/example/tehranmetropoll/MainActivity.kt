package com.example.tehranmetropoll

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tehranmetropoll.core.AppLinks
import com.example.tehranmetropoll.domain.model.ThemeMode
import com.example.tehranmetropoll.presentation.AppViewModel
import com.example.tehranmetropoll.presentation.navigation.MetroNavHost
import com.example.tehranmetropoll.presentation.splash.SplashIntroScreen
import com.example.tehranmetropoll.presentation.theme.MetroTheme

/** AppCompatActivity is required for per-app language switching on Android 12 and below. */
class MainActivity : AppCompatActivity() {

    private val appViewModel: AppViewModel by viewModels { AppViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Only a fresh launch carries a link; after recreation it was already handled.
        if (savedInstanceState == null) appViewModel.onAppLink(AppLinks.parse(intent?.data))

        // Hold the system splash until the stored theme is known, then fade it into the
        // animated intro, which starts from the same centred logo.
        splash.setKeepOnScreenCondition { appViewModel.themeMode.value == null }
        splash.setOnExitAnimationListener { provider ->
            provider.view.animate()
                .alpha(0f)
                .setDuration(SPLASH_FADE_MS)
                .withEndAction { provider.remove() }
                .start()
        }

        setContent {
            val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()
            val showIntro by appViewModel.showIntro.collectAsStateWithLifecycle()
            val pendingLink by appViewModel.pendingLink.collectAsStateWithLifecycle()
            val mode = themeMode ?: return@setContent
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            MetroTheme(darkTheme = dark) {
                Crossfade(targetState = showIntro, animationSpec = tween(600), label = "intro") { intro ->
                    if (intro) {
                        SplashIntroScreen(onFinished = appViewModel::onIntroFinished)
                    } else {
                        MetroNavHost(
                            themeMode = mode,
                            language = appViewModel.language,
                            onThemeSelected = appViewModel::onThemeSelected,
                            onLanguageSelected = appViewModel::onLanguageSelected,
                            pendingLink = pendingLink,
                            onLinkHandled = appViewModel::onAppLinkHandled,
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        appViewModel.onAppLink(AppLinks.parse(intent.data))
    }

    private companion object {
        const val SPLASH_FADE_MS = 280L
    }
}
