@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tangem.data.txhistory.list

import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.data.txhistory.fetcher.TX_HISTORY_TAG
import com.tangem.data.txhistory.list.chain.*
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.usecase.GetPaymentAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.txhistory.list.HistoryTxListManager
import com.tangem.domain.txhistory.list.HistoryTxListManager.*
import com.tangem.domain.txhistory.models.TxHistoryStateError
import com.tangem.domain.txhistory.usecase.GetTxHistoryItemsCountUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class DefaultHistoryTxListManager @AssistedInject constructor(
    dispatchers: CoroutineDispatcherProvider,
    private val expressServiceFetcher: ExpressServiceFetcher,
    private val paymentAccountCurrency: GetPaymentAccountCryptoCurrencyStatusUseCase,
    private val getAccountCryptoCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    private val txHistoryItemsCountUseCase: GetTxHistoryItemsCountUseCase,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val bsdkOnChainHistoryFactory: BsdkOnChainHistory.Factory,
    private val tangemPayOnChainHistoryFactory: TangemPayOnChainHistory.Factory,
    private val indexTableOnChainHistoryFactory: IndexTableOnChainHistory.Factory,
    @Assisted private val userWalletId: UserWalletId,
    @Assisted private val currency: CryptoCurrency,
    @Assisted private val modelScope: CoroutineScope,
) : HistoryTxListManager {

    private val actionsFlow: Channel<Action> = Channel()
    private val _historySources: MutableSharedFlow<HistorySources> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val state: StateFlow<HistoryState>
    override val historySources: Flow<HistorySources> get() = _historySources.distinctUntilChanged()

    init {
        state = buildPipeline()
            .retry { error ->
                logError(error)
                true
            }
            .runningReduce { previous, new -> if (previous.isContent && !new.isContent) previous else new }
            .flowOn(dispatchers.io)
            .stateIn(modelScope, SharingStarted.Eagerly, HistoryState.Loading)
    }

    override fun reload() {
        actionsFlow.trySend(Action.Reload(shouldRefresh = true))
    }

    override fun loadMore() {
        actionsFlow.trySend(Action.LoadMore)
    }

    private fun buildPipeline(): Flow<HistoryState> = channelFlow {
        val reloadInitialLoading = flow {
            // initial load
            emit(Unit)
            // any action triggers a reload
            emitAll(actionsFlow.receiveAsFlow().map { })
        }
        val historySources = reloadInitialLoading
            .onEach { channel.send(HistoryState.Loading) }
            .mapNotNull {
                val result = runSuspendCatching { loadSources() }.getOrNull()
                // Error state, wait for any action to reload
                if (result == null) channel.send(HistoryState.Error)
                result
            }
            .onEach { _historySources.tryEmit(it) }
            .first()
        if (!isHistoryAvailable(historySources)) {
            channel.send(HistoryState.Unavailable)
            return@channelFlow
        }

        val env = HistoryEnvironment(
            userWalletId = userWalletId,
            currency = currency,
            modelScope = modelScope,
        )
        val onChainHistory: OnChainHistory = when (historySources.onChainSource) {
            OnChainSource.BSDK -> bsdkOnChainHistoryFactory.create(env)
            OnChainSource.TangemPay -> tangemPayOnChainHistoryFactory.create(env)
            OnChainSource.IndexTable -> indexTableOnChainHistoryFactory.create(env)
        }

        actionsFlow.receiveAsFlow()
            .onEach { onChainHistory.sendAction(it) }
            .launchIn(this)

        onChainHistory.history().collect { channel.send(it) }
    }

    private suspend fun loadSources(): HistorySources = coroutineScope {
        val onChainSource = async { resolveOnChainSource() }
        val expressAsset = async { awaitExpressAsset() }

        val asset = expressAsset.await()
        HistorySources(
            onChainSource = onChainSource.await(),
            isExchangeAvailable = asset?.isExchangeAvailable == true,
            isOnrampAvailable = asset?.isOnrampAvailable == true,
        )
    }

    private fun isHistoryAvailable(sources: HistorySources): Boolean = with(sources) {
        when (onChainSource) {
            OnChainSource.BSDK -> true
            OnChainSource.TangemPay -> true
            OnChainSource.IndexTable -> isExchangeAvailable || isOnrampAvailable
        }
    }

    private suspend fun resolveOnChainSource(): OnChainSource {
        val isCryptoPortfolio = getAccountCryptoCurrencyStatusUseCase.invokeSync(userWalletId, currency).isSome()
        return when {
            isCryptoPortfolio -> txHistoryItemsCountUseCase(userWalletId, currency).fold(
                ifLeft = { error ->
                    when (error) {
                        is TxHistoryStateError.DataError -> throw error
                        TxHistoryStateError.EmptyTxHistories -> OnChainSource.BSDK
                        TxHistoryStateError.TxHistoryNotImplemented -> OnChainSource.IndexTable
                    }
                },
                ifRight = { OnChainSource.BSDK },
            )

            paymentAccountCurrency.invokeSync(userWalletId, currency).isSome() -> OnChainSource.TangemPay
            else -> OnChainSource.IndexTable
        }
    }

    private suspend fun awaitExpressAsset(): ExpressAsset? {
        val assetId = ExpressAsset.ID(currency)
        return expressServiceFetcher.getOrFetch(userWalletId, assetId).getOrNull()
    }

    private fun logError(error: Throwable) {
        val message = error.message.orEmpty()
        TangemLogger.withTag(TX_HISTORY_TAG).e(message, error)
        val event = ExceptionAnalyticsEvent(
            exception = error,
            params = mapOf("source" to TX_HISTORY_TAG),
        )
        analyticsExceptionHandler.sendException(event)
    }

    @AssistedFactory
    interface Factory : HistoryTxListManager.Factory {
        override fun create(
            userWalletId: UserWalletId,
            currency: CryptoCurrency,
            modelScope: CoroutineScope,
        ): DefaultHistoryTxListManager
    }
}