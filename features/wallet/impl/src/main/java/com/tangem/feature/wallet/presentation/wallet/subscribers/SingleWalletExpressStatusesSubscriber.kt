package com.tangem.feature.wallet.presentation.wallet.subscribers

import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.onramp.GetOnrampTransactionsUseCase
import com.tangem.domain.onramp.OnrampRemoveTransactionUseCase
import com.tangem.domain.onramp.model.cache.OnrampTransaction
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.SetExpressStatusesTransformer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import timber.log.Timber

@Suppress("LongParameterList")
internal class SingleWalletExpressStatusesSubscriber(
    private val userWallet: UserWallet,
    private val stateHolder: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val getOnrampTransactionsUseCase: GetOnrampTransactionsUseCase,
    private val onrampRemoveTransactionUseCase: OnrampRemoveTransactionUseCase,
) : WalletSubscriber() {

    override fun create(
        coroutineScope: CoroutineScope,
    ): Flow<Pair<Either<CurrencyStatusError, CryptoCurrencyStatus>, AppCurrency>> {
        return combine(
            flow = getSingleCryptoCurrencyStatusUseCase.invokeSingleWallet(userWalletId = userWallet.walletId)
                .conflate()
                .distinctUntilChanged(),
            flow2 = getSelectedAppCurrencyUseCase()
                .conflate()
                .distinctUntilChanged()
                .map { maybeAppCurrency -> maybeAppCurrency.getOrElse { AppCurrency.Default } },
            transform = { maybeCurrencyStatus, appCurrency -> maybeCurrencyStatus to appCurrency },
        ).onEach { maybeCurrencyStatusAndAppCurrency ->
            val status = maybeCurrencyStatusAndAppCurrency.first.getOrElse {
                Timber.e("Unable to get primary currency status: $it")
                return@onEach
            }

            getOnrampTransactionsUseCase(
                userWalletId = userWallet.walletId,
                cryptoCurrencyId = status.currency.id,
            ).onEach { maybeTransaction ->
                maybeTransaction.fold(
                    ifRight = { onrampTxs ->
                        onrampTxs.clearHiddenTerminal()
                        stateHolder.update(
                            SetExpressStatusesTransformer(
                                userWalletId = userWallet.walletId,
                                onrampTxs = onrampTxs,
                                clickIntents = clickIntents,
                                cryptoCurrencyStatus = status,
                                appCurrency = maybeCurrencyStatusAndAppCurrency.second,
                                analyticsEventHandler = analyticsEventHandler,
                            ),
                        )
                    },
                    ifLeft = {
                        stateHolder.update(
                            SetExpressStatusesTransformer(
                                userWalletId = userWallet.walletId,
                                onrampTxs = listOf(),
                                clickIntents = clickIntents,
                                cryptoCurrencyStatus = status,
                                appCurrency = maybeCurrencyStatusAndAppCurrency.second,
                                analyticsEventHandler = analyticsEventHandler,
                            ),
                        )
                    },
                )
            }
                .launchIn(coroutineScope)
        }
    }

    private suspend fun List<OnrampTransaction>.clearHiddenTerminal() {
        this.filter { it.status.isHidden && it.status.isTerminal }
            .forEach { onrampRemoveTransactionUseCase(txId = it.txId) }
    }
}