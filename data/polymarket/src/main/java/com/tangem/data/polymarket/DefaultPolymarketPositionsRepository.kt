package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.converter.PolymarketClosedPositionConverter
import com.tangem.data.polymarket.converter.PolymarketOpenPositionConverter
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPositionsError
import com.tangem.domain.polymarket.model.PolymarketPositionsPage
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketPositionsRepository @Inject constructor(
    private val api: PolymarketApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketPositionsRepository {

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