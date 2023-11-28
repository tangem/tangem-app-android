package com.tangem.managetokens.presentation.managetokens.state.factory

import androidx.paging.PagingData
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.state.ChooseWalletWarning
import com.tangem.managetokens.presentation.common.state.Event
import com.tangem.managetokens.presentation.common.state.WalletState
import com.tangem.managetokens.presentation.managetokens.state.AddCustomTokenButton
import com.tangem.managetokens.presentation.managetokens.state.ManageTokensState
import com.tangem.managetokens.presentation.managetokens.state.SearchBarState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.managetokens.presentation.managetokens.viewmodels.ManageTokensClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.Flow

internal class ManageTokensStateFactory(
    private val currentStateProvider: Provider<ManageTokensState>,
    private val clickIntents: ManageTokensClickIntents,
) {

    fun getInitialState(tokens: Flow<PagingData<TokenItemState>>): ManageTokensState {
        return ManageTokensState(
            searchBarState = SearchBarState(
                query = "",
                onQueryChange = clickIntents::onSearchQueryChange,
                active = false,
                onActiveChange = clickIntents::onSearchActiveChange,
            ),
            tokens = tokens,
            addCustomTokenButton = AddCustomTokenButton(
                isVisible = false,
                onClick = clickIntents::onAddCustomTokensButtonClick,
            ),
            derivationNotification = null,
            isLoading = false,
            event = consumedEvent(),
            chooseWalletState = ChooseWalletState.NoSelection,
        )
    }

    fun updateChooseWalletState(
        wallets: List<UserWallet>,
        userWallets: List<UserWallet>,
        selectedWallet: UserWallet?,
    ): ManageTokensState {
        val chooseWalletState = when {
            wallets.size == 1 -> {
                ChooseWalletState.NoSelection
            }
            wallets.isEmpty() && userWallets.all { !it.isMultiCurrency } -> {
                ChooseWalletState.Warning(ChooseWalletWarning.SINGLE_CURRENCY)
            }
            else -> {
                var selectedWalletState: WalletState? = null
                ChooseWalletState.Choose(
                    wallets = wallets.map { wallet ->
                        val walletState = WalletState(
                            walletId = wallet.walletId.stringValue,
                            artworkUrl = wallet.artworkUrl,
                            onSelected = clickIntents::onWalletSelected,
                            walletName = wallet.name,
                        )
                        if (wallet.walletId.stringValue == selectedWallet?.walletId?.stringValue) {
                            selectedWalletState = walletState
                        }
                        walletState
                    }.toPersistentList(),
                    selectedWallet = selectedWalletState,
                    onChooseWalletClick = clickIntents::onChooseWalletClick,
                    onCloseChoosingWalletClick = clickIntents::onCloseChoosingWalletClick,
                )
            }
        }
        return currentStateProvider().copy(chooseWalletState = chooseWalletState)
    }

    fun showAddCustomTokensButton(show: Boolean): ManageTokensState {
        return currentStateProvider().copy(
            addCustomTokenButton = currentStateProvider().addCustomTokenButton.copy(isVisible = show),
        )
    }

    fun updateSelectedWallet(selectedWalletId: String?): ManageTokensState {
        val chooseWalletState = currentStateProvider().chooseWalletState
        return currentStateProvider().copy(
            showChooseWalletScreen = false,
            chooseWalletState = if (chooseWalletState is ChooseWalletState.Choose) {
                chooseWalletState.copy(
                    selectedWallet = chooseWalletState.wallets.find {
                        it.walletId == selectedWalletId
                    } ?: chooseWalletState.wallets.first(),
                )
            } else {
                chooseWalletState
            },
        )
    }

    fun getStateAndTriggerEvent(
        state: ManageTokensState,
        event: Event,
        setUiState: (ManageTokensState) -> Unit,
    ): ManageTokensState {
        return state.copy(
            event = triggeredEvent(
                data = event,
                onConsume = {
                    val currentState = currentStateProvider()
                    setUiState(currentState.copy(event = consumedEvent()))
                },
            ),
        )
    }
}