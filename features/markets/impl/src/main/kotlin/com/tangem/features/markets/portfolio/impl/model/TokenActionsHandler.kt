package com.tangem.features.markets.portfolio.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.TokenReceiveBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.tokenreceive.mapToAddressModels
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.toImmutableList

@Suppress("LongParameterList")
@ComponentScoped
internal class TokenActionsHandler @AssistedInject constructor(
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val reduxStateHolder: ReduxStateHolder,
    @Assisted
    private val currentAppCurrency: Provider<AppCurrency>,
    @Assisted("updateTokenReceiveBSConfig")
    private val updateTokenReceiveBSConfig: ((TangemBottomSheetConfig) -> TangemBottomSheetConfig) -> Unit,
    @Assisted("updateTokenActionsBSConfig")
    private val updateTokenActionsBSConfig: ((TangemBottomSheetConfig) -> TangemBottomSheetConfig) -> Unit,
) {

    fun handle(action: TokenActionsBSContentUM.Action, cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        when (action) {
            TokenActionsBSContentUM.Action.Buy -> onBuyClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Exchange -> onExchangeClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Receive -> onReceiveClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.CopyAddress -> onCopyAddress(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Sell -> onSellClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Send -> onSendClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Stake -> onStakeClick(cryptoCurrencyData)
        }
    }

    fun onTokenLongClick(cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        updateTokenActionsBSConfig {
            TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = {
                    updateTokenActionsBSConfig {
                        it.copy(isShow = false)
                    }
                },
                content = TokenActionsBSContentUM(
                    title = cryptoCurrencyData.userWallet.name,
                    actions = cryptoCurrencyData.actions
                        .mapNotNull { it.toAction(cryptoCurrencyData) }
                        .sortedBy { it.order }
                        .toImmutableList(),
                    onActionClick = { action ->
                        handle(action, cryptoCurrencyData)
                        updateTokenActionsBSConfig {
                            it.copy(isShow = false)
                        }
                    },
                ),
            )
        }
    }

    private fun onReceiveClick(cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        val cryptoCurrencyStatus = cryptoCurrencyData.status
        val currency = cryptoCurrencyStatus.currency
        val networkAddress = cryptoCurrencyStatus.value.networkAddress ?: return

        updateTokenReceiveBSConfig {
            TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = {
                    updateTokenReceiveBSConfig {
                        it.copy(isShow = false)
                    }
                },
                content = TokenReceiveBottomSheetConfig(
                    name = currency.name,
                    symbol = currency.symbol,
                    network = currency.network.name,
                    addresses = networkAddress.availableAddresses
                        .mapToAddressModels(currency)
                        .toImmutableList(),
                    onCopyClick = {},
                    onShareClick = {},
                ),
            )
        }
    }

    private fun onCopyAddress(cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        val cryptoCurrencyStatus = cryptoCurrencyData.status
        val networkAddress = cryptoCurrencyStatus.value.networkAddress ?: return
        val addresses = networkAddress.availableAddresses
            .mapToAddressModels(cryptoCurrencyStatus.currency)
            .toImmutableList()
        val defaultAddress = addresses.firstOrNull()?.value ?: return

        clipboardManager.setText(text = defaultAddress)
        uiMessageSender.send(SnackbarMessage(resourceReference(R.string.wallet_notification_address_copied)))
    }

    private fun onBuyClick(cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        reduxStateHolder.dispatch(
            TradeCryptoAction.Buy(
                userWallet = cryptoCurrencyData.userWallet,
                cryptoCurrencyStatus = cryptoCurrencyData.status,
                appCurrencyCode = currentAppCurrency().code,
            ),
        )
    }

    private fun onSellClick(cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        reduxStateHolder.dispatch(
            TradeCryptoAction.Sell(
                cryptoCurrencyStatus = cryptoCurrencyData.status,
                appCurrencyCode = currentAppCurrency().code,
            ),
        )
    }

    private fun onExchangeClick(cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        router.push(
            AppRoute.Swap(
                currency = cryptoCurrencyData.status.currency,
                userWalletId = cryptoCurrencyData.userWallet.walletId,
            ),
        )
    }

    private fun onSendClick(cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        router.push(
            AppRoute.Send(
                userWalletId = cryptoCurrencyData.userWallet.walletId,
                currency = cryptoCurrencyData.status.currency,
            ),
        )
    }

    private fun onStakeClick(cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        val yield = cryptoCurrencyData.actions.firstOrNull { it is TokenActionsState.ActionState.Stake }
            ?.let { it as TokenActionsState.ActionState.Stake }
            ?.yield ?: return

        router.push(
            AppRoute.Staking(
                userWalletId = cryptoCurrencyData.userWallet.walletId,
                cryptoCurrencyId = cryptoCurrencyData.status.currency.id,
                yield = yield,
            ),
        )
    }

    private fun TokenActionsState.ActionState.toAction(
        cryptoCurrencyData: PortfolioData.CryptoCurrencyData,
    ): TokenActionsBSContentUM.Action? = when (this) {
        is TokenActionsState.ActionState.Buy -> TokenActionsBSContentUM.Action.Buy
        is TokenActionsState.ActionState.CopyAddress -> TokenActionsBSContentUM.Action.CopyAddress
        is TokenActionsState.ActionState.HideToken -> null
        is TokenActionsState.ActionState.Receive -> TokenActionsBSContentUM.Action.Receive
        is TokenActionsState.ActionState.Sell -> TokenActionsBSContentUM.Action.Sell
        is TokenActionsState.ActionState.Send -> {
            TokenActionsBSContentUM.Action.Send.takeIf {
                cryptoCurrencyData.status.value.amount.isNullOrZero().not()
            }
        }
        is TokenActionsState.ActionState.Stake -> TokenActionsBSContentUM.Action.Stake
        is TokenActionsState.ActionState.Swap -> TokenActionsBSContentUM.Action.Exchange
    }
        .takeIf { this.unavailabilityReason == ScenarioUnavailabilityReason.None }

    @AssistedFactory
    interface Factory {
        fun create(
            currentAppCurrency: Provider<AppCurrency>,
            @Assisted("updateTokenReceiveBSConfig")
            updateTokenReceiveBSConfig: ((TangemBottomSheetConfig) -> TangemBottomSheetConfig) -> Unit,
            @Assisted("updateTokenActionsBSConfig")
            updateTokenActionsBSConfig: ((TangemBottomSheetConfig) -> TangemBottomSheetConfig) -> Unit,
        ): TokenActionsHandler
    }
}