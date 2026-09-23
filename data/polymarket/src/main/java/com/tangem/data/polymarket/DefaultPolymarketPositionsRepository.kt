package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.converter.PolymarketClosedPositionConverter
import com.tangem.data.polymarket.converter.PolymarketOpenPositionConverter
import com.tangem.data.polymarket.pagination.PolymarketPositionsBatchFetcher
import com.tangem.data.polymarket.pagination.PolymarketPositionsUpdateFetcher
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsListConfig
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketPositionsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketPositionsTab
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.data.PolymarketDataApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.toBatchFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import javax.inject.Inject

internal class DefaultPolymarketPositionsRepository @Inject constructor(
    private val api: PolymarketApi,
    private val dataApi: PolymarketDataApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketPositionsRepository {

    override fun getPositionsBatchFlow(
        context: PolymarketPositionsBatchingContext,
        batchSize: Int,
    ): PolymarketPositionsBatchFlow = BatchListSource(
        fetchDispatcher = dispatchers.io,
        context = context,
        generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
        batchFetcher = PolymarketPositionsBatchFetcher(batchSize = batchSize, fetchPage = ::fetchPage),
        updateFetcher = PolymarketPositionsUpdateFetcher(batchSize = batchSize, fetchPage = ::fetchPage),
    ).toBatchFlow()

    private suspend fun fetchPage(
        config: PolymarketPositionsListConfig,
        limit: Int,
        offset: Int,
    ): Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition>> = when (config.tab) {
        PolymarketPositionsTab.ACTIVE -> getOpenPositions(config.addresses, limit, offset)
        PolymarketPositionsTab.FINISHED -> getClosedPositions(config.addresses, limit, offset)
    }

    override suspend fun getOpenPositions(
        addresses: PolymarketAddresses,
        limit: Int,
        offset: Int,
    ): Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition.Open>> = read(
        call = {
            api.getPositions(
                walletId = addresses.userWalletId.stringValue,
                depositWalletAddress = addresses.depositWalletAddress,
                limit = limit,
                offset = offset,
            )
        },
        convert = { response ->
            PolymarketPositionsPage(PolymarketOpenPositionConverter.convertList(response.positions), response.hasNext)
        },
    )

    override suspend fun getClosedPositions(
        addresses: PolymarketAddresses,
        limit: Int,
        offset: Int,
    ): Either<PolymarketPositionsError, PolymarketPositionsPage<PolymarketPosition.Closed>> = read(
        call = {
            api.getClosedPositions(
                walletId = addresses.userWalletId.stringValue,
                depositWalletAddress = addresses.depositWalletAddress,
                limit = limit,
                offset = offset,
            )
        },
        convert = { response ->
            PolymarketPositionsPage(PolymarketClosedPositionConverter.convertList(response.positions), response.hasNext)
        },
    )

    override suspend fun getPosition(
        addresses: PolymarketAddresses,
        id: PolymarketPositionId,
    ): Either<PolymarketPositionsError, PolymarketPosition.Open?> = read(
        call = {
            api.getPosition(
                walletId = addresses.userWalletId.stringValue,
                depositWalletAddress = addresses.depositWalletAddress,
                conditionId = id.conditionId,
                assetId = id.assetId,
            )
        },
        convert = { it.position?.let(PolymarketOpenPositionConverter::convert) },
    )

    override suspend fun getPositionsValue(
        addresses: PolymarketAddresses,
    ): Either<PolymarketPositionsError, BigDecimal> = read(
        call = { dataApi.getPositionsValue(user = addresses.depositWalletAddress) },
        convert = { it.firstOrNull()?.value ?: BigDecimal.ZERO },
    )

    private suspend fun <R : Any, T> read(
        call: suspend () -> ApiResponse<R>,
        convert: (R) -> T,
    ): Either<PolymarketPositionsError, T> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = call().bind()
                Either.catch { convert(response) }
                    .mapLeft { PolymarketPositionsError.Unknown(httpCode = null, detail = it.message) }
            },
            onError = { it.toPositionsError().left() },
        )
    }

    private fun ApiResponseError.toPositionsError(): PolymarketPositionsError = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PolymarketPositionsError.Network
        is ApiResponseError.HttpException -> PolymarketPositionsError.Unknown(code.numericCode, errorBody)
        is ApiResponseError.UnknownException -> PolymarketPositionsError.Unknown(null, cause.message ?: message)
    }
}