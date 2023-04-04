package com.tangem.feature.tester.presentation.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.lib.crypto.UserWalletManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TesterActionsViewModel @Inject constructor(
    private val userWalletManager: UserWalletManager,
) : ViewModel() {

    var uiState: TesterActionsContentState by mutableStateOf(initialState)
        private set

    private val initialState: TesterActionsContentState
        get() = TesterActionsContentState(
            onBackClick = { /* TODO */ },
            hideAllCurrencies = HideAllCurrenciesState.Clickable(this::hideAllCurrencies),
        )

    fun setupNavigation(router: InnerTesterRouter) {
        uiState = uiState.copy(onBackClick = router::back)
    }

    private fun hideAllCurrencies() = viewModelScope.launch {
        uiState = uiState.copy(
            hideAllCurrencies = HideAllCurrenciesState.Progress,
        )
        userWalletManager.hideAllTokens()

        uiState = uiState.copy(
            hideAllCurrencies = HideAllCurrenciesState.Clickable(this@TesterActionsViewModel::hideAllCurrencies),
        )
    }
}
