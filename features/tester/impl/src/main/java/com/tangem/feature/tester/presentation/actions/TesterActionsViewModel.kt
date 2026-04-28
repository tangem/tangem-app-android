package com.tangem.feature.tester.presentation.actions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.repository.FeedbackRepository
import com.tangem.domain.settings.HotWalletRestrictionManager
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.HideAllCurrenciesUM
import com.tangem.feature.tester.presentation.actions.TesterActionsContentState.ToggleHotWalletRestrictionUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class TesterActionsViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val walletAccountsSaver: WalletAccountsSaver,
    private val hotWalletRestrictionManager: HotWalletRestrictionManager,
) : ViewModel() {

    var uiState: TesterActionsContentState by mutableStateOf(initialState)
        private set

    private val initialState: TesterActionsContentState
        get() = TesterActionsContentState(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Clickable(this::hideAllCurrencies),
            toggleHotWalletRestrictionUM = ToggleHotWalletRestrictionUM(
                isEnabled = hotWalletRestrictionManager.isCreationEnabledSync(),
                onClick = this::toggleHotWalletRestriction,
            ),
            shareLogsUM = TesterActionsContentState.ShareLogsUM(file = feedbackRepository.getLogFile()),
            onBackClick = { /* no-op */ },
        )

    init {
        bootstrapHotWalletRestrictionUpdates()
    }

    fun setupNavigation(router: InnerTesterRouter) {
        uiState = uiState.copy(onBackClick = router::back)
    }

    private fun hideAllCurrencies() = viewModelScope.launch {
        uiState = uiState.copy(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Progress,
        )

        val userWalletId = userWalletsListRepository.selectedUserWalletSync()?.walletId

        if (userWalletId != null) {
            walletAccountsSaver.update(userWalletId = userWalletId) { response ->
                response ?: return@update response

                response.copy(
                    accounts = response.accounts.map { accountDTO ->
                        accountDTO.copy(tokens = emptyList())
                    },
                )
            }
        }

        uiState = uiState.copy(
            hideAllCurrenciesUM = HideAllCurrenciesUM.Clickable(this@TesterActionsViewModel::hideAllCurrencies),
        )
    }

    private fun toggleHotWalletRestriction() = viewModelScope.launch {
        hotWalletRestrictionManager.toggleCreationEnabled()
    }

    private fun bootstrapHotWalletRestrictionUpdates() {
        hotWalletRestrictionManager.isCreationEnabled()
            .onEach { isEnabled ->
                uiState = uiState.copy(
                    toggleHotWalletRestrictionUM = uiState.toggleHotWalletRestrictionUM.copy(
                        isEnabled = isEnabled,
                    ),
                )
            }
            .launchIn(viewModelScope)
    }
}