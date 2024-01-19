package com.tangem.managetokens.presentation.managetokens.state.factory

import androidx.paging.PagingData
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.domain.tokens.CurrencyCompatibilityError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.managetokens.presentation.common.state.*
import com.tangem.managetokens.presentation.common.utils.CurrencyUtils
import com.tangem.managetokens.presentation.managetokens.state.*
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

    fun transformAddTokenErrorToAlert(error: CurrencyCompatibilityError, networkName: String): AlertState {
        return when (error) {
            CurrencyCompatibilityError.SolanaTokensUnsupported -> AlertState.TokensUnsupported(networkName)
            CurrencyCompatibilityError.UnsupportedBlockchain -> AlertState.TokensUnsupportedBlockchainByCard(
                networkName,
            )
            CurrencyCompatibilityError.UnsupportedCurve -> AlertState.TokensUnsupportedCurve
        }
    }

    fun toggleNetworkState(
        token: TokenItemState.Loaded,
        network: NetworkItemState.Toggleable,
        allAddedCurrencies: Collection<CryptoCurrency>,
    ) {
        network.changeToggleState()
        val anyNetworkAdded = isAnyNetworkAdded(
            networks = token.chooseNetworkState.nativeNetworks + token.chooseNetworkState.nonNativeNetworks,
            allAddedCurrencies = allAddedCurrencies,
        )
        val buttonType = if (anyNetworkAdded) TokenButtonType.EDIT else TokenButtonType.ADD
        token.availableAction.value = buttonType
    }

    private fun isAnyNetworkAdded(
        networks: List<NetworkItemState>,
        allAddedCurrencies: Collection<CryptoCurrency>,
    ): Boolean {
        return networks.any {
            it is NetworkItemState.Toggleable && CurrencyUtils.isAdded(
                address = it.address,
                networkId = it.id,
                currencies = allAddedCurrencies,
            )
        }
    }

    fun updateTokenNetworksOnTokenSelection(
        token: TokenItemState.Loaded,
        addedCurrenciesOnWallet: Collection<CryptoCurrency>,
    ) {
        token.chooseNetworkState.nativeNetworks.forEach {
            if (it is NetworkItemState.Toggleable) {
                val isAdded = CurrencyUtils.isAdded(it.address, it.id, addedCurrenciesOnWallet)
                if (isAdded != it.isAdded.value) (it as? NetworkItemState.Toggleable)?.changeToggleState()
            }
        }
        token.chooseNetworkState.nonNativeNetworks.forEach {
            if (it is NetworkItemState.Toggleable) {
                val isAdded = CurrencyUtils.isAdded(it.address, it.id, addedCurrenciesOnWallet)
                if (isAdded != it.isAdded.value) (it as? NetworkItemState.Toggleable)?.changeToggleState()
            }
        }
    }

    fun updateDerivationNotification(totalNeeded: Int, totalWallets: Int, walletsToDerive: Int): ManageTokensState {
        val derivationNotificationState = if (totalNeeded == 0) {
            null
        } else {
            DerivationNotificationState(
                totalNeeded = totalNeeded,
                totalWallets = totalWallets,
                walletsToDerive = walletsToDerive,
                onGenerateClick = clickIntents::onGenerateDerivationClick,
            )
        }
        return currentStateProvider().copy(derivationNotification = derivationNotificationState)
    }
}