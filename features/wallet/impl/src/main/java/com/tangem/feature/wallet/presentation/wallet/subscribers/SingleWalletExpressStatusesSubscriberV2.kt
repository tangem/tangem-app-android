package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.GetOnrampTransactionsUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.feature.wallet.child.wallet.model.ModelScopeDependencies
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountsSharedFlowHolder
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetExpressStatusesTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class SingleWalletExpressStatusesSubscriberV2(
    override val userWallet: UserWallet,
    val modelScopeDependencies: ModelScopeDependencies,
    override val accountsSharedFlowHolder: AccountsSharedFlowHolder = modelScopeDependencies.accountsSharedFlowHolder,
    private val getOnrampTransactionsUseCase: GetOnrampTransactionsUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : BasicSingleWalletSubscriber() {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        val getOnrampTransactionsFlow = getPrimaryCurrencyStatusFlow().flatMapLatest { currencyStatus ->
            getOnrampTransactionsUseCase(
                userWalletId = userWallet.walletId,
                cryptoCurrencyId = currencyStatus.currency.id,
            )
                .map { currencyStatus to it }
        }

        return combine(
            flow = getOnrampTransactionsFlow,
            flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
            transform = ::toTriple,
        )
            .onEach { (status, maybeTransaction, appCurrency) ->
                maybeTransaction.fold(
                    ifRight = { onrampTxs ->
                        onrampTxs.clearHiddenTerminal()
                        stateController.update(
                            SetExpressStatusesTransformer(
                                userWalletId = userWallet.walletId,
                                onrampTxs = onrampTxs,
                                clickIntents = clickIntents,
                                cryptoCurrencyStatus = status,
                                appCurrency = appCurrency,
                                analyticsEventHandler = analyticsEventHandler,
                            ),
                        )
                    },
                    ifLeft = {
                        stateController.update(
                            SetExpressStatusesTransformer(
                                userWalletId = userWallet.walletId,
                                onrampTxs = listOf(),
                                clickIntents = clickIntents,
                                cryptoCurrencyStatus = status,
                                appCurrency = appCurrency,
                                analyticsEventHandler = analyticsEventHandler,
                            ),
                        )
                    },
                )
            }
    }

    private fun <A, B, C> toTriple(firstPair: Pair<A, B>, second: C): Triple<A, B, C> {
        return Triple(firstPair.first, firstPair.second, second)
    }

    private suspend fun List<OnrampTransaction>.clearHiddenTerminal() {
        this
            .filter { it.status.isHidden && it.status.isTerminal }
            .forEach { onrampRemoveTransactionUseCase(txId = it.txId) }
    }
}