package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.model.analytics.TokenExchangeAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenOnrampAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenScreenAnalyticsEvent
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.model.ExpressTransactionsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.components.ExchangeStatusNotification
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class TokenDetailsExpressStatusFactory @AssistedInject constructor(
    @Assisted private val currentStateProvider: Provider<TokenDetailsState>,
    @Assisted private val clickIntents: ExpressTransactionsClickIntents,
    @Assisted private val cryptoCurrency: CryptoCurrency,
    @Assisted appCurrencyProvider: Provider<AppCurrency>,
    @Assisted userWallet: UserWallet,
    @Assisted cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    tokenDetailsOnrampStatusFactory: TokenDetailsOnrampStatusFactory.Factory,
    tokenDetailsExchangeStatusFactory: TokenDetailsExchangeStatusFactory.Factory,
) {

    private val exchangeStatusFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        tokenDetailsExchangeStatusFactory.create(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            userWallet = userWallet,
            cryptoCurrency = cryptoCurrency,
        )
    }

    private val onrampStatusFactory by lazy(LazyThreadSafetyMode.NONE) {
        tokenDetailsOnrampStatusFactory.create(
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
            clickIntents = clickIntents,
            cryptoCurrency = cryptoCurrency,
            userWallet = userWallet,
        )
    }

    fun getExpressStatuses(): Flow<PersistentList<ExpressTransactionStateUM>> = combine(
        flow = exchangeStatusFactory(),
        flow2 = onrampStatusFactory(),
    ) { maybeExchange, maybeOnramp ->
        persistentListOf(maybeOnramp, maybeExchange)
            .flatten()
            .sortedByDescending { it.info.timestamp }
            .toPersistentList()
    }

    suspend fun getUpdatedExpressStatuses(expressTxs: PersistentList<ExpressTransactionStateUM>) =
        withContext(dispatchers.io) {
            expressTxs.map { tx ->
                async {
                    when (tx) {
                        is ExchangeUM -> exchangeStatusFactory.updateSwapTxStatus(tx)
                        is ExpressTransactionStateUM.OnrampUM -> onrampStatusFactory.updateOnrmapTxStatus(tx)
                        else -> null
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .toPersistentList()
        }

    fun getStateWithUpdatedExpressTxs(
        expressTxs: PersistentList<ExpressTransactionStateUM>,
        displayedTxId: String?,
        updateBalance: (CryptoCurrency) -> Unit,
    ): TokenDetailsState {
        val state = currentStateProvider()
        val currentTx = displayedTxId?.let { txId ->
            expressTxs.firstOrNull { it.info.txId == txId }
        }
        if (currentTx is ExchangeUM && currentTx.activeStatus == ExchangeStatus.Finished) {
            updateBalance(currentTx.toCryptoCurrency)
        }
        val expressTxsToDisplay = expressTxs.filterNot { txs ->
            when (txs) {
                is ExpressTransactionStateUM.OnrampUM -> txs.activeStatus.isHidden
                else -> false
            }
        }.toPersistentList()
        return state.copy(
            expressTxs = expressTxs,
            expressTxsToDisplay = expressTxsToDisplay,
        )
    }

    fun sendExpressStatusAnalytics(expressState: ExpressTransactionStateUM) {
        val analyticEvents = when (expressState) {
            is ExchangeUM -> listOfNotNull(
                TokenExchangeAnalyticsEvent.CexTxStatusOpened(
                    token = cryptoCurrency.symbol,
                    provider = expressState.provider.name,
                ),
                maybeGetLongTimeExchangeNotificationShowEvent(
                    expressState = expressState,
                    currentStateNotification = null,
                    isBottomSheetShown = true,
                ),
            )
            is ExpressTransactionStateUM.OnrampUM -> listOf(
                TokenOnrampAnalyticsEvent.OnrampStatusOpened(
                    tokenSymbol = cryptoCurrency.symbol,
                    provider = expressState.providerName,
                    fiatCurrency = expressState.fromCurrencyCode,
                ),
            )
            else -> return
        }

        analyticEvents.forEach { analyticsEventsHandler.send(it) }
    }

    suspend fun removeTransactionOnBottomSheetClosed(
        expressState: ExpressTransactionStateUM,
        isForceDispose: Boolean = false,
    ) {
        when (expressState) {
            is ExchangeUM -> exchangeStatusFactory.removeTransactionOnBottomSheetClosed(expressState, isForceDispose)
            is ExpressTransactionStateUM.OnrampUM -> onrampStatusFactory.removeTransactionOnBottomSheetClosed(
                expressState,
                isForceDispose,
            )
        }
    }

    fun maybeGetLongTimeExchangeNotificationShowEvent(
        expressState: ExpressTransactionStateUM,
        currentStateNotification: ExchangeStatusNotification?,
        isBottomSheetShown: Boolean,
    ): TokenScreenAnalyticsEvent? {
        val newState = expressState as? ExchangeUM
        val newStateNotification = newState?.notification
        return if (currentStateNotification !is ExchangeStatusNotification.LongTimeExchange &&
            newStateNotification is ExchangeStatusNotification.LongTimeExchange &&
            isBottomSheetShown
        ) {
            TokenExchangeAnalyticsEvent.LongTimeTransaction(
                token = cryptoCurrency.symbol,
                provider = newState.provider.name,
            )
        } else {
            null
        }
    }

    @AssistedFactory
    interface Factory {
        @Suppress("LongParameterList")
        fun create(
            clickIntents: ExpressTransactionsClickIntents,
            appCurrencyProvider: Provider<AppCurrency>,
            currentStateProvider: Provider<TokenDetailsState>,
            userWallet: UserWallet,
            cryptoCurrency: CryptoCurrency,
            cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
        ): TokenDetailsExpressStatusFactory
    }
}