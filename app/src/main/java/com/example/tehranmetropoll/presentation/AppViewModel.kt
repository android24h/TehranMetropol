package com.example.tehranmetropoll.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tehranmetropoll.MetroApplication
import com.example.tehranmetropoll.core.AppLink
import com.example.tehranmetropoll.domain.model.AppLanguage
import com.example.tehranmetropoll.domain.model.ThemeMode
import com.example.tehranmetropoll.domain.usecase.GetLanguageUseCase
import com.example.tehranmetropoll.domain.usecase.ObserveThemeModeUseCase
import com.example.tehranmetropoll.domain.usecase.SetLanguageUseCase
import com.example.tehranmetropoll.domain.usecase.SetThemeModeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Activity-scoped settings: appearance and language. */
class AppViewModel(
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val getLanguage: GetLanguageUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val onLanguageChanged: () -> Unit,
) : ViewModel() {

    /** Null until the stored preference is read, so the first frame never flashes the wrong theme. */
    val themeMode: StateFlow<ThemeMode?> = observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * The animated intro plays once per launch. Held here (not in the activity) so a language
     * change, which recreates the activity, does not replay it.
     */
    private val _showIntro = MutableStateFlow(true)
    val showIntro: StateFlow<Boolean> = _showIntro.asStateFlow()

    val language: AppLanguage get() = getLanguage()

    /** A screen requested by a notification or the home-screen widget, waiting to be opened. */
    private val _pendingLink = MutableStateFlow<AppLink?>(null)
    val pendingLink: StateFlow<AppLink?> = _pendingLink.asStateFlow()

    fun onAppLink(link: AppLink?) {
        if (link == null) return
        // Opening straight onto a trip should not sit behind the intro animation.
        _showIntro.value = false
        _pendingLink.value = link
    }

    fun onAppLinkHandled() {
        _pendingLink.value = null
    }

    fun onIntroFinished() {
        _showIntro.value = false
    }

    fun onThemeSelected(mode: ThemeMode) {
        viewModelScope.launch { setThemeMode(mode) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        if (language != getLanguage()) {
            setLanguage(language)
            onLanguageChanged()
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as MetroApplication).container
                AppViewModel(c.observeThemeMode, c.setThemeMode, c.getLanguage, c.setLanguage, c::onLanguageChanged)
            }
        }
    }
}
