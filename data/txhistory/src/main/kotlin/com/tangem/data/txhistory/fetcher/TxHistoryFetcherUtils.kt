package com.tangem.data.txhistory.fetcher

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.domain.txhistory.fetcher.TxHistoryFetchTrigger
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.plus
import javax.inject.Inject

const val TX_HISTORY_TAG = "TxHistory"

internal interface TxHistoryFetcherUtils {

    val triggersBuffer: Channel<TxHistoryFetchTrigger>

    val fetcherScope: CoroutineScope
    val analyticsEventHandler: AnalyticsEventHandler
    val analyticsExceptionHandler: AnalyticsExceptionHandler

    suspend fun sendTrigger(trigger: TxHistoryFetchTrigger)

    companion object {

        fun TxHistoryFetcherUtils.cancelScope() = fetcherScope.cancel()

        fun <T> TxHistoryFetcherUtils.defaultLaunchIn(flow: Flow<T>) = flow
            .retry { error ->
                logError(error)
                true
            }
            .launchIn(fetcherScope)

        fun TxHistoryFetcherUtils.receiveTrigger(): Flow<TxHistoryFetchTrigger> {
            return triggersBuffer.receiveAsFlow()
        }

        inline fun <reified R> TxHistoryFetcherUtils.receiveTriggerInstance(): Flow<R> {
            return receiveTrigger().filterIsInstance<R>()
        }

        fun logError(error: Throwable, message: String = error.message.orEmpty()) {
            TangemLogger.withTag(TX_HISTORY_TAG).e(message, error)
        }
    }
}

internal class DefaultTxHistoryFetcherUtils @Inject constructor(
    appScope: AppCoroutineScope,
    override val analyticsEventHandler: AnalyticsEventHandler,
    override val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : TxHistoryFetcherUtils {

    override val triggersBuffer: Channel<TxHistoryFetchTrigger> = Channel(Channel.BUFFERED)

    // todo txhistory use lifecycle scope?
    override val fetcherScope: CoroutineScope = appScope + SupervisorJob()

    override suspend fun sendTrigger(trigger: TxHistoryFetchTrigger) {
        triggersBuffer.trySend(trigger)
    }
}