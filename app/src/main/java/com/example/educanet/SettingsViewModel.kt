package com.example.educanet

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUi(
    val rememberSession: Boolean = true,
    val darkMode: Boolean = false,
    val avatarUri: String? = null
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val ds = SettingsDataStore(app)

    val ui: StateFlow<SettingsUi> =
        kotlinx.coroutines.flow.combine(
            ds.rememberSession, ds.darkMode, ds.avatarUri
        ) { r, d, a -> SettingsUi(r, d, a) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUi())

    fun setRemember(v: Boolean) = viewModelScope.launch { ds.setRememberSession(v) }
    fun setDark(v: Boolean) = viewModelScope.launch { ds.setDarkMode(v) }
    fun setAvatar(uri: String?) = viewModelScope.launch { ds.setAvatar(uri) }
}
