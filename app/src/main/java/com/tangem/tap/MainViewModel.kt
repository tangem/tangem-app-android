package com.tangem.tap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.model.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class MainViewModel @Inject constructor(
    private val getAppThemeModeUseCase: GetAppThemeModeUseCase,
) : ViewModel() {

    val state: StateFlow<GlobalSettingsState> = createMainStateFlow()

    private fun createMainStateFlow(): StateFlow<GlobalSettingsState> {
        return getAppThemeModeUseCase()
            .map { maybeMode ->
                val mode = maybeMode.getOrElse { AppThemeMode.DEFAULT }

                GlobalSettingsState.Content(appThemeMode = mode)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
                initialValue = GlobalSettingsState.Loading,
            )
    }
}