package com.tangem.data.common.quote

import androidx.annotation.VisibleForTesting
import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.common.quote.QuotesFetcher.Error
import com.tangem.data.common.quote.QuotesFetcher.Field
import com.tangem.data.common.quote.utils.combine
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.QuotesResponse
import com.tangem.domain.core.utils.eitherOn
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

/**
 * Default implementation of [QuotesFetcher]
 *
 * @property tangemTechApi Tangem tech API
 * @property dispatchers   dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultQuotesFetcher(
    private val tangemTechApi: TangemTechApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : QuotesFetcher {

    /** Mutex for synchronization of the [getFetchMutex] method */
    private val getFetchMethodMutex = Mutex()

    /**
     * Map for tracking active [fetch] methods.
     * The key is the parameters of the [fetch] method, and the value is [Mutex].
     */
    private val fetchMutexMap = ConcurrentHashMap<RequestParams, Mutex>()

    /**
     * Cache that stores a list of quotes for a certain fiat currency.
     * Quotes are considered expired if they are in the cache for more than [tenSecInMillis] seconds.
     */
    private val quotesCache = ConcurrentHashMap<String, Set<QuoteMetadata>>()

    override suspend fun fetch(
        fiatCurrencyId: String,
        currenciesIds: Set<String>,
        fields: Set<Field>,
    ): Either<Error, QuotesResponse> = eitherOn(dispatchers.default) {
        val validatedParams = validateParams(fiatCurrencyId, currenciesIds, fields)

        if (validatedParams.currenciesIds.isEmpty()) return@eitherOn emptyQuotesResponse

        val mutex = getFetchMutex(params = validatedParams)

        return@eitherOn fetch(params = validatedParams, mutex = mutex)
    }
        .onLeft {
            val params = RequestParams(fiatCurrencyId, currenciesIds, fields)
            fetchMutexMap.remove(params)
        }

    private fun Raise<Error>.validateParams(
        fiatCurrencyId: String,
        currenciesIds: Set<String>,
        fields: Set<Field>,
    ): RequestParams {
        ensure(fiatCurrencyId.isNotBlank() && fields.isNotEmpty()) {
            raise(Error.InvalidArgumentsError)
        }

        val filterCurrenciesIds = currenciesIds.filter(String::isNotEmpty).toSet()

        return RequestParams(fiatCurrencyId = fiatCurrencyId, currenciesIds = filterCurrenciesIds, fields = fields)
    }

    /**
     * The method for determining the [Mutex], which will be used by the [fetch] method.
     *
     * @param params request params
     *
     * @return if at the moment the method of [fetch] is already executed for an adjacent set of parameters

     */
    private suspend fun Raise<Error>.getFetchMutex(params: RequestParams): Mutex {
        return getFetchMethodMutex.withLock {
            val similarJobsMutexes = fetchMutexMap.filterKeys { metadata ->
                metadata.fiatCurrencyId == params.fiatCurrencyId &&
                    params.currenciesIds.any { it in metadata.currenciesIds }
            }

            val storedMutex = similarJobsMutexes.firstNotNullOfOrNull { it.value }

            when {
                storedMutex == null || !storedMutex.isLocked -> Mutex()
                storedMutex.isLocked -> storedMutex
                else -> raise(Error.CacheOperationError)
            }
                .also { fetchMutexMap[params] = it }
        }
    }

    /**
     * Fetch quotes by [params].
     *
     * It works in the scope of the transferred [mutex].
     * If at the moment [Mutex] is busy, then it is working on fetching related data, then the current request will
     * wait for its execution.
     *
     * @return [QuotesResponse]
     */
    private suspend fun Raise<Error>.fetch(params: RequestParams, mutex: Mutex): QuotesResponse {
        val (fiatCurrencyId, currenciesIds) = params

        return mutex.withLock {
            val quotes = quotesCache[fiatCurrencyId].orEmpty()

            val expiredOrAbsentIds = currenciesIds.filterExpiredOrAbsent(quotes)

            // We will fetch quotes only for those quotes that are absent in cache or expired
            if (expiredOrAbsentIds.isNotEmpty()) {
                val response = requestQuotes(
                    fiatCurrencyId = fiatCurrencyId,
                    currenciesIds = expiredOrAbsentIds.toSet(),
                    fields = params.fields,
                )

                saveQuotes(fiatCurrencyId = fiatCurrencyId, response = response)
            }

            fetchMutexMap.remove(params)

            getCachedResult(fiatCurrencyId = fiatCurrencyId, currenciesIds = currenciesIds)
        }
    }

    private fun Set<String>.filterExpiredOrAbsent(quotes: Set<QuoteMetadata>?): List<String> {
        return filter { id ->
            val quote = quotes?.firstOrNull { it.cryptoCurrencyId == id && !it.isExpired }

            quote == null
        }
    }

    private fun Raise<Error>.getCachedResult(fiatCurrencyId: String, currenciesIds: Set<String>): QuotesResponse {
        val storedQuotes = quotesCache[fiatCurrencyId]

        ensureNotNull(storedQuotes) {
            raise(Error.CacheOperationError)
        }

        val quotes = catch(
            block = {
                currenciesIds.associateWith { currencyId ->
                    storedQuotes.first { it.cryptoCurrencyId == currencyId }.value
                }
            },
            catch = { raise(Error.CacheOperationError) },
        )

        return QuotesResponse(quotes = quotes)
    }

    private suspend fun Raise<Error>.requestQuotes(
        fiatCurrencyId: String,
        currenciesIds: Set<String>,
        fields: Set<Field>,
    ) = withContext(dispatchers.io) {
        safeApiCallWithTimeout(
            call = {
                tangemTechApi.getQuotes(
                    currencyId = fiatCurrencyId,
                    coinIds = currenciesIds.joinToString(separator = ","),
                    fields = fields.combine(),
                )
                    .bind()
            },
            onError = { raise(Error.ApiOperationError(it)) },
        )
    }

    private fun saveQuotes(fiatCurrencyId: String, response: QuotesResponse) {
        val newQuotes = response.quotes.mapTo(destination = hashSetOf()) { (currencyId, quote) ->
            QuoteMetadata(
                cryptoCurrencyId = currencyId,
                timestamp = DateTime.now().millis,
                value = quote,
            )
        }

        val storedQuotes = quotesCache[fiatCurrencyId].orEmpty()

        val storedUniqueQuotes = storedQuotes.filterNot { stored ->
            newQuotes.any { stored.cryptoCurrencyId == it.cryptoCurrencyId }
        }

        quotesCache[fiatCurrencyId] = (storedUniqueQuotes + newQuotes).toSet()
    }

    data class RequestParams(
        val fiatCurrencyId: String,
        val currenciesIds: Set<String>,
        val fields: Set<Field>,
    )

    data class QuoteMetadata(
        val cryptoCurrencyId: String,
        val timestamp: Long,
        val value: QuotesResponse.Quote,
    ) {

        val isExpired: Boolean
            get() = DateTime.now().millis - timestamp > tenSecInMillis
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun getCachedQuotes() = quotesCache

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setCachedQuotes(fiatCurrencyId: String, quotes: Set<QuoteMetadata>) {
        quotesCache[fiatCurrencyId] = quotes
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearCache() {
        quotesCache.clear()
    }

    private companion object {
        val emptyQuotesResponse = QuotesResponse(quotes = emptyMap())
        val tenSecInMillis = 10.seconds.inWholeMilliseconds
    }
}