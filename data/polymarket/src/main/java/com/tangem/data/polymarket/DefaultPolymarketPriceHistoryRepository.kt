package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.converter.PolymarketPriceHistoryConverter
import com.tangem.domain.polymarket.PolymarketPriceHistoryRepository
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryError
import com.tangem.domain.polymarket.model.PolymarketPriceHistoryQuery
import com.tangem.domain.polymarket.model.PolymarketPriceInterval
import com.tangem.domain.polymarket.model.PolymarketPricePoint
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketPriceHistoryRepository @Inject constructor(
    private val api: PolymarketClobApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketPriceHistoryRepository {

    override suspend fun getPriceHistory(
        assetId: String,
        query: PolymarketPriceHistoryQuery,
    ): Either<PolymarketPriceHistoryError, List<PolymarketPricePoint>> = withContext(dispatchers.io) {
        if (assetId.isBlank()) return@withContext PolymarketPriceHistoryError.InvalidToken.left()
        val interval = query.window as? PolymarketPriceHistoryQuery.Window.Interval
        val range = query.window as? PolymarketPriceHistoryQuery.Window.Range
        safeApiCall(
            call = {
                val response = api.getPriceHistory(
                    tokenId = assetId,
                    fidelityMinutes = query.fidelityMinutes,
                    interval = interval?.interval?.wireValue,
                    startSeconds = range?.start?.epochSecond,
                    endSeconds = range?.end?.epochSecond,
                ).bind()
                Either.catch { PolymarketPriceHistoryConverter.convert(response) }
                    .mapLeft { PolymarketPriceHistoryError.Unknown(httpCode = null, detail = it.message) }
            },
            onError = { it.toHistoryError().left() },
        )
    }

    private fun ApiResponseError.toHistoryError(): PolymarketPriceHistoryError = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketPriceHistoryError.Network
        is ApiResponseError.HttpException -> PolymarketPriceHistoryError.Unknown(code.numericCode, errorBody)
        is ApiResponseError.UnknownException -> PolymarketPriceHistoryError.Unknown(null, cause.message ?: message)
    }
}

private val PolymarketPriceInterval.wireValue: String
    get() = when (this) {
        PolymarketPriceInterval.HOUR -> "1h"
        PolymarketPriceInterval.SIX_HOURS -> "6h"
        PolymarketPriceInterval.DAY -> "1d"
        PolymarketPriceInterval.WEEK -> "1w"
        PolymarketPriceInterval.MONTH -> "1m"
        PolymarketPriceInterval.MAX -> "max"
    }