package com.tangem.feature.tester.presentation.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.domain.apptheme.ChangeAppThemeModeUseCase
import com.tangem.domain.apptheme.GetAppThemeModeUseCase
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.HideAllCurrenciesUM
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.ToggleAppThemeUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.lib.crypto.UserWalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class TesterActionsViewModel @Inject constructor(
    private val userWalletManager: UserWalletManager,
    private val changeAppThemeModeUseCase: ChangeAppThemeModeUseCase,
    private val getAppThemeModeUseCase: GetAppThemeModeUseCase,
    private val feedbackRepository: FeedbackRepository,
) : ViewModel() {

    var uiState: TesterActionsContentState by mutableStateOf(initialState)
        private set

    private val initialState: TesterActionsContentState
        get() = TesterActionsContentState(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Clickable(this::hideAllCurrencies),
            toggleAppThemeUM = ToggleAppThemeUM(
                currentAppTheme = AppThemeMode.DEFAULT,
                onClick = this::toggleAppTheme,
            ),
            shareLogsUM = TesterActionsContentState.ShareLogsUM(file = feedbackRepository.getLogFile()),
            onBackClick = { /* no-op */ },
        )

    init {
        bootstrapAppThemeModeUpdates()
    }

    fun setupNavigation(router: InnerTesterRouter) {
        uiState = uiState.copy(onBackClick = router::back)
    }

    private fun hideAllCurrencies() = viewModelScope.launch {
        uiState = uiState.copy(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Progress,
        )
        userWalletManager.hideAllTokens()

        uiState = uiState.copy(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Clickable(this@TesterActionsViewModel::hideAllCurrencies),
        )
    }

    private fun toggleAppTheme() = viewModelScope.launch {
        val currentAppThemeMode = uiState.toggleAppThemeUM.currentAppTheme
        val newAppThemeMode = when (currentAppThemeMode) {
            AppThemeMode.FORCE_DARK -> AppThemeMode.FORCE_LIGHT
            AppThemeMode.FORCE_LIGHT -> AppThemeMode.FOLLOW_SYSTEM
            AppThemeMode.FOLLOW_SYSTEM -> AppThemeMode.FORCE_DARK
        }

        Timber.d(
            """
            Change app theme mode
            |- Current theme mode: $currentAppThemeMode
            |- New theme mode: $newAppThemeMode
            """.trimIndent(),
        )

        changeAppThemeModeUseCase(newAppThemeMode).onLeft { error ->
            Timber.e(
                """
                Unable to change app theme mode
                |- Error: $error
                """.trimIndent(),
            )
        }
    }

    private fun bootstrapAppThemeModeUpdates() {
        getAppThemeModeUseCase()
            .distinctUntilChanged()
            .onEach { maybeAppThemeMode ->
                Timber.d(
                    """
                    Current app theme mode updated
                    |- Previous app theme mode: ${uiState.toggleAppThemeUM.currentAppTheme}
                    |- New app theme mode: $maybeAppThemeMode
                    """.trimIndent(),
                )

                uiState = uiState.copy(
                    toggleAppThemeUM = uiState.toggleAppThemeUM.copy(
                        currentAppTheme = maybeAppThemeMode.getOrElse { error ->
                            Timber.e(
                                """
                                Unable to get current app theme mode, using default
                                |- Default theme mode: ${AppThemeMode.DEFAULT}
                                |- Error: $error
                                """.trimIndent(),
                            )

                            AppThemeMode.DEFAULT
                        },
                    ),
                )
            }
            .launchIn(viewModelScope)
    }
}