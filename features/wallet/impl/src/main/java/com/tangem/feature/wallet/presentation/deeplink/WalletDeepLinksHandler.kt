package com.tangem.feature.wallet.presentation.deeplink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.deeplink.DeepLink
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.global.BuyCurrencyDeepLink
import com.tangem.core.deeplink.global.SellCurrencyDeepLink
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.legacy.TradeCryptoAction
import com.tangem.domain.tokens.legacy.TradeCryptoAction.TransactionInfo
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import com.tangem.features.onramp.OnrampFeatureToggles
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
    private val getCryptoCurrencyStatusesSyncUseCase: GetCryptoCurrencyStatusesSyncUseCase,
    private val getFeePaidCryptoCurrencyStatusSyncUseCase: GetFeePaidCryptoCurrencyStatusSyncUseCase,
    private val getNetworkCoinStatusUseCase: GetNetworkCoinStatusUseCase,
    private val reduxStateHolder: ReduxStateHolder,
    private val clickIntents: WalletClickIntents,
    private val onrampFeatureToggles: OnrampFeatureToggles,
) {

    private var deepLinksMap = mutableMapOf<UserWalletId, List<DeepLink>>()

    fun registerForWallet(viewModel: ViewModel, userWallet: UserWallet) {
        val deepLinks = deepLinksMap.getOrPut(userWallet.walletId) {
            getDeepLinks(userWallet, viewModel.viewModelScope)
        }
        deepLinksRegistry.unregisterByIds(deepLinks.map { it.id })
        deepLinksRegistry.register(deepLinks)

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

        return buildList {
            add(sellCurrencyDeepLink)
            if (onrampFeatureToggles.isFeatureEnabled || !userWallet.isMultiCurrency) {
                add(
                    BuyCurrencyDeepLink(
                        onReceive = { externalTxId ->
                            scope.launch { onBuyCurrencyDeepLink(externalTxId, userWallet) }
                        },
                    ),
                )
            }
        }
    }

    private suspend fun onSellCurrencyDeepLink(userWallet: UserWallet, data: SellCurrencyDeepLink.Data) {
        val cryptoCurrencyStatus = findCryptoCurrencyStatus(userWallet, data.currencyId) ?: return
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

    private suspend fun onBuyCurrencyDeepLink(externalTxId: String, userWallet: UserWallet) {
        if (onrampFeatureToggles.isFeatureEnabled) {
            clickIntents.onOnrampSuccessClick(externalTxId)
        } else {
            val cryptoCurrency = getCryptoCurrencyUseCase(userWallet.walletId).getOrNull() ?: return
            analyticsEventHandler.send(TokenScreenAnalyticsEvent.Bought(cryptoCurrency.symbol))
        }
    }

    private suspend fun findCryptoCurrencyStatus(
        userWallet: UserWallet,
        currencyIdValue: String,
    ): CryptoCurrencyStatus? {
        return if (userWallet.isMultiCurrency) {
            getCryptoCurrencyStatusesSyncUseCase(userWallet.walletId).getOrNull()?.let { currencies ->
                currencies.find { currencyIdValue == it.currency.id.value }
            }
        } else {
            getCryptoCurrencyStatusSyncUseCase(userWallet.walletId).getOrNull()
        }
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