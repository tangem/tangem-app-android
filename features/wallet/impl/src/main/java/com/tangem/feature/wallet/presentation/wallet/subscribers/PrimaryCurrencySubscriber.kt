package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.settings.SetWalletWithFundsFoundUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetPrimaryCurrencyTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.math.BigDecimal

internal class PrimaryCurrencySubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val setWalletWithFundsFoundUseCase: SetWalletWithFundsFoundUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : WalletSubscriber() {

    override fun create(
        coroutineScope: CoroutineScope,
    ): Flow<Pair<Either<CurrencyStatusError, CryptoCurrencyStatus>, AppCurrency>> {
        return combine(
            flow = getSingleCryptoCurrencyStatusUseCase.invokeSingleWallet(userWallet.walletId)
                .conflate()
                .distinctUntilChanged(),
            flow2 = getSelectedAppCurrencyUseCase()
                .conflate()
                .distinctUntilChanged()
                .map { maybeAppCurrency -> maybeAppCurrency.getOrElse { AppCurrency.Default } },
            transform = { maybeCurrencyStatus, appCurrency -> maybeCurrencyStatus to appCurrency },
        )
            .onEach { maybeCurrencyStatusAndAppCurrency ->
                val status = maybeCurrencyStatusAndAppCurrency.first.getOrElse {
                    Timber.e("Unable to get primary currency status: $it")
                    return@onEach
                }

                updateContent(status, maybeCurrencyStatusAndAppCurrency.second)
                sendAnalyticsEvent(status)
                checkWalletWithFunds(status)
            }
    }

    private fun updateContent(status: CryptoCurrencyStatus, appCurrency: AppCurrency) {
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
            is CryptoCurrencyStatus.Unreachable,
            -> AnalyticsParam.CardBalanceState.BlockchainError
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Loading,
            is CryptoCurrencyStatus.Custom,
            -> null
        }

        cardBalanceState?.let {
            // do not send tokens count for single currency wallet
            analyticsEventHandler.send(
                event = WalletScreenAnalyticsEvent.Basic.BalanceLoaded(
                    balance = it,
                    tokensCount = null,
                ),
            )
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