package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.converter.PolymarketActivityConverter
import com.tangem.data.polymarket.pagination.PolymarketActivityBatchFetcher
import com.tangem.domain.polymarket.PolymarketActivityRepository
import com.tangem.domain.polymarket.model.PolymarketActivityBatchFlow
import com.tangem.domain.polymarket.model.PolymarketActivityBatchingContext
import com.tangem.domain.polymarket.model.PolymarketActivityError
import com.tangem.domain.polymarket.model.PolymarketActivityListConfig
import com.tangem.domain.polymarket.model.PolymarketActivityPage
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class DefaultPolymarketActivityRepository @Inject constructor(
    private val api: PolymarketApi,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketActivityRepository {

    override fun getActivityBatchFlow(
        context: PolymarketActivityBatchingContext,
        batchSize: Int,
    ): PolymarketActivityBatchFlow = BatchListSource(
        fetchDispatcher = dispatchers.io,
        context = context,
        generateNewKey = { keys -> keys.size },
        batchFetcher = PolymarketActivityBatchFetcher(batchSize, ::getActivity),
    ).toBatchFlow()

    override suspend fun getActivity(
        config: PolymarketActivityListConfig,
        cursor: String?,
        limit: Int,
    ): Either<PolymarketActivityError, PolymarketActivityPage> = read(
        call = {
            api.getActivity(
                walletId = config.addresses.userWalletId.stringValue,
                depositWalletAddress = config.addresses.depositWalletAddress,
                type = config.filter.type?.name,
                redeemResult = config.filter.redeemResult?.name,
                limit = limit,
                cursor = cursor,
            )
        },
        convert = { PolymarketActivityPage(PolymarketActivityConverter.convertList(it.items), it.nextCursor) },
    )

    override suspend fun reconcileDeposits(addresses: PolymarketAddresses): Either<PolymarketActivityError, Duration> =
        read(
            call = { api.reconcileDeposits(addresses.userWalletId.stringValue, addresses.depositWalletAddress) },
            convert = { response ->
                require(response.state == "SCANNING") { "Unexpected deposit reconciliation state: ${response.state}" }
                require(response.retryAfterSeconds >= 0) { "Negative deposit reconciliation delay" }
                response.retryAfterSeconds.seconds
            },
        )

    private suspend fun <R : Any, T> read(
        call: suspend () -> ApiResponse<R>,
        convert: (R) -> T,
    ): Either<PolymarketActivityError, T> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = call().bind()
                Either.catch { convert(response) }
                    .mapLeft { PolymarketActivityError.Unknown(httpCode = null, detail = it.message) }
            },
            onError = { it.toActivityError().left() },
        )
    }

    private fun ApiResponseError.toActivityError(): PolymarketActivityError = when (this) {
        is ApiResponseError.NetworkException, is ApiResponseError.TimeoutException -> PolymarketActivityError.Network
        is ApiResponseError.HttpException -> PolymarketActivityError.Unknown(code.numericCode, errorBody)
        is ApiResponseError.UnknownException -> PolymarketActivityError.Unknown(null, cause.message ?: message)
    }
}