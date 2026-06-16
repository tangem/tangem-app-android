package com.tangem.common.ui.markets.action

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.bottomsheet.receive.mapToAddressModels
import com.tangem.common.ui.markets.R
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OfframpAnalyticsEvent
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.offramp.GetOfframpUrlUseCase
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.utils.Provider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.toImmutableList

@Suppress("LongParameterList")
class TokenActionsHandler @AssistedInject constructor(
    private val router: Router,
    private val clipboardManager: ClipboardManager,
    private val uiMessageSender: UiMessageSender,
    private val getOfframpUrlUseCase: GetOfframpUrlUseCase,
    private val urlOpener: UrlOpener,
    private val analyticsEventHandler: AnalyticsEventHandler,
    @Assisted private val currentAppCurrency: Provider<AppCurrency>,
    @Assisted private val onHandleQuickAction: (action: HandledQuickAction, shouldDismiss: Boolean) -> Unit,
    @Assisted private val coroutineScope: CoroutineScope,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val messageSender: UiMessageSender,
) {

    private val disabledActionsInDemoMode = buildSet {
        add(TokenActionsBSContentUM.Action.Sell)
    }

    fun handle(action: TokenActionsBSContentUM.Action, cryptoCurrencyData: CryptoCurrencyData) {
        onHandleQuickAction(
            HandledQuickAction(
                action = action,
                cryptoCurrencyData = cryptoCurrencyData,
            ),
            when (action) {
                TokenActionsBSContentUM.Action.Receive,
                TokenActionsBSContentUM.Action.CopyAddress,
                TokenActionsBSContentUM.Action.Sell,
                -> false
                TokenActionsBSContentUM.Action.Send,
                TokenActionsBSContentUM.Action.Stake,
                TokenActionsBSContentUM.Action.YieldMode,
                TokenActionsBSContentUM.Action.Buy,
                TokenActionsBSContentUM.Action.Exchange,
                -> true
            },
        )
        val userWallet = cryptoCurrencyData.userWallet
        if (userWallet is UserWallet.Cold && handleDemoMode(action, userWallet)) return

        when (action) {
            TokenActionsBSContentUM.Action.Buy -> onBuyClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Exchange -> onExchangeClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Receive -> Unit
            TokenActionsBSContentUM.Action.CopyAddress -> onCopyAddress(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Sell -> onSellClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Send -> onSendClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.Stake -> onStakeClick(cryptoCurrencyData)
            TokenActionsBSContentUM.Action.YieldMode -> onYieldModeClick(cryptoCurrencyData)
        }
    }

    private fun handleDemoMode(action: TokenActionsBSContentUM.Action, userWallet: UserWallet.Cold): Boolean {
        val isDemoCard = isDemoCardUseCase.invoke(userWallet.cardId)
        val isNeededShowDemoWarning = isDemoCard && disabledActionsInDemoMode.contains(action)

        if (isNeededShowDemoWarning) {
            showDemoModeWarning()
        }

        return isNeededShowDemoWarning
    }

    private fun showDemoModeWarning() {
        val message = DialogMessage.Companion(
            message = resourceReference(R.string.alert_demo_feature_disabled),
        )
        messageSender.send(message)
    }

    private fun onCopyAddress(cryptoCurrencyData: CryptoCurrencyData) {
        val cryptoCurrencyStatus = cryptoCurrencyData.status
        val networkAddress = cryptoCurrencyStatus.value.networkAddress ?: return
        val addresses = networkAddress.availableAddresses
            .mapToAddressModels(cryptoCurrencyStatus.currency)
            .toImmutableList()
        val defaultAddress = addresses.firstOrNull()?.value ?: return

        clipboardManager.setText(text = defaultAddress, isSensitive = true)
        uiMessageSender.send(SnackbarMessage(resourceReference(R.string.wallet_notification_address_copied)))
    }

    private fun onBuyClick(cryptoCurrencyData: CryptoCurrencyData) {
        router.push(
            AppRoute.Onramp(
                userWalletId = cryptoCurrencyData.userWallet.walletId,
                currency = cryptoCurrencyData.status.currency,
                source = OnrampSource.MARKETS,
            ),
        )
    }

    private fun onSellClick(cryptoCurrencyData: CryptoCurrencyData) {
        coroutineScope.launch {
            getOfframpUrlUseCase(
                userWalletId = cryptoCurrencyData.userWallet.walletId,
                cryptoCurrencyStatus = cryptoCurrencyData.status,
                appCurrencyCode = currentAppCurrency().code,
            ).onRight { url ->
                urlOpener.openUrl(url)
                analyticsEventHandler.send(OfframpAnalyticsEvent.ScreenOpened)
            }
        }
    }

    private fun onExchangeClick(cryptoCurrencyData: CryptoCurrencyData) {
        router.push(
            AppRoute.Swap(
                fromCryptoCurrency = cryptoCurrencyData.status.currency,
                userWalletId = cryptoCurrencyData.userWallet.walletId,
                screenSource = AnalyticsParam.ScreensSources.Markets.value,
            ),
        )
    }

    private fun onSendClick(cryptoCurrencyData: CryptoCurrencyData) {
        val route = AppRoute.SendEntryPoint(
            userWalletId = cryptoCurrencyData.userWallet.walletId,
            currency = cryptoCurrencyData.status.currency,
        )
        router.push(route)
    }

    private fun onStakeClick(cryptoCurrencyData: CryptoCurrencyData) {
        val option = cryptoCurrencyData.actions.firstOrNull { it is TokenActionsState.ActionState.Stake }
            ?.let { it as TokenActionsState.ActionState.Stake }
            ?.option ?: return

        router.push(
            AppRoute.Staking(
                userWalletId = cryptoCurrencyData.userWallet.walletId,
                cryptoCurrency = cryptoCurrencyData.status.currency,
                integrationId = option.integrationId,
            ),
        )
    }

    private fun onYieldModeClick(cryptoCurrencyData: CryptoCurrencyData) {
        val yieldSupplyApy = cryptoCurrencyData.actions.filterIsInstance<TokenActionsState.ActionState.YieldMode>()
            .firstOrNull()?.apy ?: return

        router.push(
            AppRoute.YieldSupplyEntry(
                userWalletId = cryptoCurrencyData.userWallet.walletId,
                cryptoCurrency = cryptoCurrencyData.status.currency,
                apy = yieldSupplyApy,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(
            currentAppCurrency: Provider<AppCurrency>,
            onHandleQuickAction: (HandledQuickAction, shouldDismiss: Boolean) -> Unit,
            coroutineScope: CoroutineScope,
        ): TokenActionsHandler
    }

    data class HandledQuickAction(
        val action: TokenActionsBSContentUM.Action,
        val cryptoCurrencyData: CryptoCurrencyData,
    )
}