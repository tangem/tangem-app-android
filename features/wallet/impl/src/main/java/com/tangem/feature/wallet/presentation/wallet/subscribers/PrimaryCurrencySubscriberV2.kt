package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.ModelScopeDependencies
import com.tangem.feature.wallet.presentation.account.AccountsSharedFlowHolder
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetPrimaryCurrencyTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import java.math.BigDecimal

internal class PrimaryCurrencySubscriberV2(
    override val userWallet: UserWallet,
    val modelScopeDependencies: ModelScopeDependencies,
    override val accountsSharedFlowHolder: AccountsSharedFlowHolder = modelScopeDependencies.accountsSharedFlowHolder,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val stateController: WalletStateController,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : BasicSingleWalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return combine(
            flow = getPrimaryCurrencyStatusFlow(),
            flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
            transform = ::Pair,
        )
            .onEach { (status, appCurrency) ->
                updateContent(status, appCurrency)
                sendAnalyticsEvent(status)
            }
    }

    private fun updateContent(status: CryptoCurrencyStatus, appCurrency: AppCurrency) {
        stateController.update(
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
}