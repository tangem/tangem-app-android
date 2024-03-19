package com.tangem.feature.wallet.presentation.deeplink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.deeplink.DeepLink
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.global.BuyCurrencyDeepLink
import com.tangem.core.deeplink.global.SellCurrencyDeepLink
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetCryptoCurrencyUseCase
import com.tangem.domain.tokens.GetFeePaidCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.GetNetworkCoinStatusUseCase
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.legacy.TradeCryptoAction.TransactionInfo
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.models.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class WalletDeepLinksHandler @Inject constructor(
    private val deepLinksRegistry: DeepLinksRegistry,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getCryptoCurrencyUseCase: GetCryptoCurrencyUseCase,
    private val getCryptoCurrencyStatusSyncUseCase: GetCryptoCurrencyStatusSyncUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val reduxStateHolder: ReduxStateHolder,
) {

    private var deepLinks: List<DeepLink> = emptyList()

    fun registerForSingleCurrencyWallets(viewModel: ViewModel, userWallet: UserWallet) {
        if (userWallet.isMultiCurrency) {
            deepLinksRegistry.unregister(deepLinks)
        } else {
            if (deepLinks.isEmpty()) {
                deepLinks = getDeepLinks(userWallet, viewModel.viewModelScope)
            }

            deepLinksRegistry.register(deepLinks)
        }

        viewModel.addCloseable {
            deepLinksRegistry.unregister(deepLinks)
        }
    }

    private fun getDeepLinks(userWallet: UserWallet, scope: CoroutineScope): List<DeepLink> {
        val sellCurrencyDeepLink = SellCurrencyDeepLink(
            onReceive = { data ->
                scope.launch {
                    onSellCurrencyDeepLink(userWallet, data)
                }
            },
        )
        val buyCurrencyDeepLink = BuyCurrencyDeepLink(
            onReceive = {
                scope.launch {
                    onBuyCurrencyDeepLink(userWallet)
                }
            },
        )

        return listOf(sellCurrencyDeepLink, buyCurrencyDeepLink)
    }

    private suspend fun onSellCurrencyDeepLink(userWallet: UserWallet, data: SellCurrencyDeepLink.Data) {
        val cryptoCurrencyStatus = getCryptoCurrencyStatusSyncUseCase(userWallet.walletId)
            .getOrNull() ?: return
        val feeCurrencyStatus = getFeePaidCryptoCurrencyStatusSyncUseCase(
            userWallet.walletId,
            cryptoCurrencyStatus,
        ).getOrNull()

        val transactionInfo = data.let {
            TransactionInfo(
                amount = it.baseCurrencyAmount,
                destinationAddress = it.depositWalletAddress,
                transactionId = it.transactionId,
                tag = it.depositWalletAddressTag,
            )
        }

        when (cryptoCurrencyStatus.currency) {
            is CryptoCurrency.Coin -> sendCoin(
                userWallet = userWallet,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                feeCurrencyStatus = feeCurrencyStatus,
                transactionInfo = transactionInfo,
            )
            is CryptoCurrency.Token -> sendToken(
                userWallet = userWallet,
                cryptoCurrencyStatus = cryptoCurrencyStatus,
                feeCurrencyStatus = feeCurrencyStatus,
                transactionInfo = transactionInfo,
            )
        }
    }

    private suspend fun onBuyCurrencyDeepLink(userWallet: UserWallet) {
        val cryptoCurrency = getCryptoCurrencyUseCase(userWallet.walletId).getOrNull() ?: return

        analyticsEventHandler.send(TokenScreenAnalyticsEvent.Bought(cryptoCurrency.symbol))
    }

    private fun sendCoin(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeCurrencyStatus: CryptoCurrencyStatus?,
        transactionInfo: TransactionInfo,
    ) {
        reduxStateHolder.dispatch(
            action = TradeCryptoAction.SendCoin(
                userWallet = userWallet,
                coinStatus = cryptoCurrencyStatus,
                feeCurrencyStatus = feeCurrencyStatus,
                transactionInfo = transactionInfo,
            ),
        )
    }

    private suspend fun sendToken(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        feeCurrencyStatus: CryptoCurrencyStatus?,
        transactionInfo: TransactionInfo,
    ) {
        val cryptoCurrency = cryptoCurrencyStatus.currency as? CryptoCurrency.Token ?: return
        val coinStatus = getNetworkCoinStatusUseCase(
            userWalletId = userWallet.walletId,
            networkId = cryptoCurrency.network.id,
            derivationPath = cryptoCurrency.network.derivationPath,
            isSingleWalletWithTokens = false,
        )
            .firstOrNull()
            ?.getOrNull()
            ?: return

        reduxStateHolder.dispatch(
            action = TradeCryptoAction.SendToken(
                userWallet = userWallet,
                tokenCurrency = cryptoCurrency,
                tokenFiatRate = cryptoCurrencyStatus.value.fiatRate,
                coinFiatRate = coinStatus.value.fiatRate,
                feeCurrencyStatus = feeCurrencyStatus,
                transactionInfo = transactionInfo,
            ),
        )
    }
}
