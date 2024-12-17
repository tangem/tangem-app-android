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
import com.tangem.core.ui.message.ContentMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.features.markets.impl.R
import com.tangem.features.markets.portfolio.impl.loader.PortfolioData
import com.tangem.features.markets.portfolio.impl.ui.WarningDialog
import com.tangem.features.markets.portfolio.impl.ui.state.TokenActionsBSContentUM
import com.tangem.features.onramp.OnrampFeatureToggles
import com.tangem.utils.Provider
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
    @Assisted private val currentAppCurrency: Provider<AppCurrency>,
    @Assisted private val updateTokenReceiveBSConfig: ((TangemBottomSheetConfig) -> TangemBottomSheetConfig) -> Unit,
    @Assisted private val onHandleQuickAction: (HandledQuickAction) -> Unit,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val messageSender: UiMessageSender,
    private val onrampFeatureToggles: OnrampFeatureToggles,
) {

    private val disabledActionsInDemoMode = buildSet {
        if (!onrampFeatureToggles.isFeatureEnabled) {
            add(TokenActionsBSContentUM.Action.Buy)
        }
        add(TokenActionsBSContentUM.Action.Sell)
    }

    fun handle(action: TokenActionsBSContentUM.Action, cryptoCurrencyData: PortfolioData.CryptoCurrencyData) {
        onHandleQuickAction(
            HandledQuickAction(
                action = action,
                cryptoCurrencyData = cryptoCurrencyData,
            ),
        )
        if (handleDemoMode(action, cryptoCurrencyData.userWallet)) return

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

    private fun handleDemoMode(action: TokenActionsBSContentUM.Action, userWallet: UserWallet): Boolean {
        val demoCard = isDemoCardUseCase.invoke(userWallet.cardId)
        val needShowDemoWarning = demoCard && disabledActionsInDemoMode.contains(action)

        if (needShowDemoWarning) {
            showDemoModeWarning()
        }

        return needShowDemoWarning
    }

    private fun showDemoModeWarning() {
        val message = ContentMessage { onDismiss ->
            WarningDialog(
                message = resourceReference(R.string.alert_demo_feature_disabled),
                onDismiss = onDismiss,
            )
        }

        messageSender.send(message)
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
                    showMemoDisclaimer = currency.network.transactionExtrasType != Network.TransactionExtrasType.NONE,
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
                source = OnrampSource.MARKETS,
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
                currencyFrom = cryptoCurrencyData.status.currency,
                userWalletId = cryptoCurrencyData.userWallet.walletId,
                isInitialReverseOrder = true,
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

    @AssistedFactory
    interface Factory {
        fun create(
            currentAppCurrency: Provider<AppCurrency>,
            updateTokenReceiveBSConfig: ((TangemBottomSheetConfig) -> TangemBottomSheetConfig) -> Unit,
            onHandleQuickAction: (HandledQuickAction) -> Unit,
        ): TokenActionsHandler
    }

    data class HandledQuickAction(
        val action: TokenActionsBSContentUM.Action,
        val cryptoCurrencyData: PortfolioData.CryptoCurrencyData,
    )
}