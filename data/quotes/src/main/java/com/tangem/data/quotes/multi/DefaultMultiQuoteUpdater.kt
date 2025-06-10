package com.tangem.data.quotes.multi

import androidx.annotation.VisibleForTesting
import arrow.core.left
import com.tangem.data.quotes.store.QuotesStatusesStore
import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.domain.core.utils.EitherFlow
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.multi.MultiQuoteFetcher
import com.tangem.domain.quotes.multi.MultiQuoteUpdater
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Default implementation of [MultiQuoteUpdater] which updates quotes when the app currency changes
 *
 * @property appCurrencyResponseStore app currency response store
 * @property quotesStore              quotes store
 * @property multiQuoteFetcher        multi quote fetcher
 * @param dispatchers                 dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class DefaultMultiQuoteUpdater @Inject constructor(
    private val appCurrencyResponseStore: AppCurrencyResponseStore,
    private val quotesStore: QuotesStatusesStore,
    private val multiQuoteFetcher: MultiQuoteFetcher,
    dispatchers: CoroutineDispatcherProvider,
) : MultiQuoteUpdater {

    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val updaterHolder = JobHolder()

    override fun subscribe() {
        Timber.d("Subscribe on quotes updates")
        getMultiQuoteUpdates()
            .launchIn(coroutineScope)
            .saveIn(updaterHolder)
    }

    override fun unsubscribe() {
        Timber.e("Unsubscribe from quotes updates")
        updaterHolder.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getMultiQuoteUpdates(): EitherFlow<Throwable, Unit> {
        return appCurrencyResponseStore.get()
            .drop(count = 1) // skip initial value
            .distinctUntilChanged()
            .filterNotNull()
            .mapLatest { appCurrency ->
                val currenciesIds = quotesStore.getAllSyncOrNull().orEmpty()
                    .mapTo(destination = hashSetOf(), transform = QuoteStatus::rawCurrencyId)

                multiQuoteFetcher(
                    params = MultiQuoteFetcher.Params(currenciesIds = currenciesIds, appCurrencyId = appCurrency.id),
                )
                    .onLeft(Timber::e)
            }
            .retryWhen { cause, _ ->
                Timber.e("Retry updating quotes: $cause")

                emit(cause.left())

                delay(timeMillis = 2000)
                true
            }
    }

    @VisibleForTesting(VisibleForTesting.NONE)
    fun getMultiQuoteUpdatesFlow(): EitherFlow<Throwable, Unit> = getMultiQuoteUpdates()

    @VisibleForTesting(VisibleForTesting.NONE)
    fun getUpdaterJobHolder(): JobHolder = updaterHolder
}