package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetPrimaryCurrencyTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import java.math.BigDecimal

internal class PrimaryCurrencySubscriber(
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val stateHolder: WalletStateController,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return getPrimaryCurrencyStatusUpdatesUseCase(userWallet.walletId)
            .conflate()
            .distinctUntilChanged()
            .onEach { maybeCurrencyStatus ->
                val status = maybeCurrencyStatus.getOrElse {
                    Timber.e("Unable to get primary currency status: $it")
                    return@onEach
                }

                updateContent(status)
                sendAnalyticsEvent(status)
                checkWalletWithFunds(status)
            }
    }

    private fun updateContent(status: CryptoCurrencyStatus) {
        stateHolder.update(
            SetPrimaryCurrencyTransformer(
                status = status,
                userWallet = userWallet,
                appCurrency = appCurrency,
            ),
        )
    }

    private fun sendAnalyticsEvent(status: CryptoCurrencyStatus) {
        val fiatAmount = status.value.fiatAmount
        val cardBalanceState = when (status.value) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.NoAccount,
            is CryptoCurrencyStatus.NoAmount,
            -> createCardBalanceState(fiatAmount)
            is CryptoCurrencyStatus.NoQuote -> AnalyticsParam.CardBalanceState.NoRate
            is CryptoCurrencyStatus.Unreachable -> AnalyticsParam.CardBalanceState.BlockchainError
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Loading,
            is CryptoCurrencyStatus.Custom,
            -> null
        }

        cardBalanceState?.let {
            analyticsEventHandler.send(event = WalletScreenAnalyticsEvent.Basic.BalanceLoaded(balance = it))
        }
    }

    private fun createCardBalanceState(fiatAmount: BigDecimal?): AnalyticsParam.CardBalanceState? {
        return when {
            fiatAmount == null -> null
            fiatAmount.isZero() -> AnalyticsParam.CardBalanceState.Empty
            else -> AnalyticsParam.CardBalanceState.Full
        }
    }

    private suspend fun checkWalletWithFunds(status: CryptoCurrencyStatus) {
        if (status.value.amount?.isZero() == false) setWalletWithFundsFoundUseCase()
    }
}