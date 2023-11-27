package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
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
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateHolderV2
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.SetPrimaryCurrencyTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import java.math.BigDecimal
import kotlin.coroutines.CoroutineContext

internal class PrimaryCurrencySubscriber(
    private val userWallet: UserWallet,
    private val appCurrency: AppCurrency,
    private val stateHolder: WalletStateHolderV2,
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : WalletSubscriber<Either<CurrencyStatusError, CryptoCurrencyStatus>>(name = "primary_currency") {

    override fun create(
        coroutineScope: CoroutineScope,
        uiDispatcher: CoroutineContext,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return getPrimaryCurrencyStatusUpdatesUseCase(userWallet.walletId)
            .conflate()
            .distinctUntilChanged()
            .onEach(::updateContent)
            .onEach(::sendAnalyticsEvent)
            .onEach(::checkWalletWithFunds)
    }

    private fun updateContent(maybeCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>) {
        val status = (maybeCurrencyStatus as? Either.Right)?.value ?: return

        stateHolder.update(
            SetPrimaryCurrencyTransformer(
                status = status.value,
                userWallet = userWallet,
                appCurrency = appCurrency,
            ),
        )
    }

    private fun sendAnalyticsEvent(maybeCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>) {
        val status = (maybeCurrencyStatus as? Either.Right)?.value ?: return

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

    private suspend fun checkWalletWithFunds(maybeCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>) {
        val status = (maybeCurrencyStatus as? Either.Right)?.value ?: return

        if (status.value.amount?.isZero() == false) setWalletWithFundsFoundUseCase()
    }
}
