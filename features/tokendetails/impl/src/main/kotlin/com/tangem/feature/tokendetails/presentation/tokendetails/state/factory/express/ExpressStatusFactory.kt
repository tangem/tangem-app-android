package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory.express

import com.tangem.common.ui.expressStatus.ExpressStatusBottomSheetConfig
import com.tangem.common.ui.expressStatus.state.ExpressTransactionStateUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.analytics.TokenExchangeAnalyticsEvent
import com.tangem.domain.tokens.model.analytics.TokenOnrampAnalyticsEvent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.feature.swap.domain.models.domain.ExchangeStatus
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.express.ExchangeUM
import com.tangem.feature.tokendetails.presentation.tokendetails.viewmodels.TokenDetailsClickIntents
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
internal class ExpressStatusFactory @AssistedInject constructor(
    @Assisted private val currentStateProvider: Provider<TokenDetailsState>,
    @Assisted private val clickIntents: TokenDetailsClickIntents,
    @Assisted private val cryptoCurrency: CryptoCurrency,
    @Assisted appCurrencyProvider: Provider<AppCurrency>,
    @Assisted userWalletId: UserWalletId,
    @Assisted cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventsHandler: AnalyticsEventHandler,
    onrampStatusFactory: OnrampStatusFactory.Factory,
    exchangeStatusFactory: ExchangeStatusFactory.Factory,
) {

    private val exchangeStatusFactory by lazy(mode = LazyThreadSafetyMode.NONE) {
        exchangeStatusFactory.create(
            clickIntents = clickIntents,
            appCurrencyProvider = appCurrencyProvider,
            currentStateProvider = currentStateProvider,
            userWalletId = userWalletId,
            cryptoCurrency = cryptoCurrency,
        )
    }

    private val onrampStatusFactory by lazy(LazyThreadSafetyMode.NONE) {
        onrampStatusFactory.create(
            currentStateProvider = currentStateProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
            appCurrencyProvider = appCurrencyProvider,
            clickIntents = clickIntents,
            cryptoCurrency = cryptoCurrency,
            userWalletId = userWalletId,
        )
    }

    suspend fun getExpressStatuses(): Flow<PersistentList<ExpressTransactionStateUM>> = combine(
        flow = exchangeStatusFactory(),
        flow2 = onrampStatusFactory(),
    ) { maybeExchange, maybeOnramp ->
        persistentListOf(
            maybeOnramp,
            maybeExchange,
        ).flatten()
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
        updateBalance: (CryptoCurrency) -> Unit,
    ): TokenDetailsState {
        val state = currentStateProvider()
        val config = state.bottomSheetConfig
        val expressBottomSheet = config?.content as? ExpressStatusBottomSheetConfig
        val currentTx = expressTxs.firstOrNull { it.info.txId == expressBottomSheet?.value?.info?.txId }
        if (currentTx is ExchangeUM && currentTx.activeStatus == ExchangeStatus.Finished) {
            updateBalance(currentTx.toCryptoCurrency)
        }
        val expressTxsToDisplay = expressTxs.filterNot {
            when (it) {
                is ExpressTransactionStateUM.OnrampUM -> false // it.activeStatus.isHidden
                else -> false
            }
        }.toPersistentList()
        return state.copy(
            expressTxs = expressTxs,
            expressTxsToDisplay = expressTxsToDisplay,
            bottomSheetConfig = currentTx?.let(
                ::updateStateWithExpressStatusBottomSheet,
            ) ?: config,
        )
    }

    fun getStateWithExpressStatusBottomSheet(expressState: ExpressTransactionStateUM): TokenDetailsState {
        val analyticEvent = when (expressState) {
            is ExchangeUM -> TokenExchangeAnalyticsEvent.CexTxStatusOpened(
                cryptoCurrency.symbol,
            )
            is ExpressTransactionStateUM.OnrampUM -> TokenOnrampAnalyticsEvent.OnrampStatusOpened(
                tokenSymbol = cryptoCurrency.symbol,
                provider = expressState.providerName,
                fiatCurrency = expressState.fromCurrencyCode,
            )
            else -> return currentStateProvider()
        }

        analyticsEventsHandler.send(analyticEvent)

        return currentStateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShow = true,
                onDismissRequest = clickIntents::onDismissBottomSheet,
                content = ExpressStatusBottomSheetConfig(
                    value = expressState,
                ),
            ),
        )
    }

    fun updateStateWithExpressStatusBottomSheet(expressState: ExpressTransactionStateUM): TangemBottomSheetConfig? {
        val state = currentStateProvider()
        val bottomSheetConfig = state.bottomSheetConfig
        val currentConfig = bottomSheetConfig?.content as? ExpressStatusBottomSheetConfig ?: return bottomSheetConfig
        return bottomSheetConfig.copy(
            content = if (currentConfig.value != expressState) {
                ExpressStatusBottomSheetConfig(expressState)
            } else {
                currentConfig
            },
        )
    }

    suspend fun removeTransactionOnBottomSheetClosed(
        expressState: ExpressTransactionStateUM,
        isForceTerminal: Boolean = false,
    ) {
        when (expressState) {
            is ExchangeUM -> exchangeStatusFactory.removeTransactionOnBottomSheetClosed(isForceTerminal)
            is ExpressTransactionStateUM.OnrampUM -> onrampStatusFactory.removeTransactionOnBottomSheetClosed()
        }
    }

    @AssistedFactory
    interface Factory {
        @Suppress("LongParameterList")
        fun create(
            clickIntents: TokenDetailsClickIntents,
            appCurrencyProvider: Provider<AppCurrency>,
            currentStateProvider: Provider<TokenDetailsState>,
            userWalletId: UserWalletId,
            cryptoCurrency: CryptoCurrency,
            cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
        ): ExpressStatusFactory
    }
}