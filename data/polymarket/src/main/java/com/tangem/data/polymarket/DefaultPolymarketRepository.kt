package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.polymarket.converter.PolymarketApiKeyConverter
import com.tangem.data.polymarket.converter.PolymarketBalanceAllowanceConverter
import com.tangem.data.polymarket.converter.PolymarketEventConverter
import com.tangem.data.polymarket.converter.PolymarketWalletConverter
import com.tangem.data.polymarket.builder.PolymarketOrderTypedDataBuilder
import com.tangem.data.polymarket.builder.PolymarketPlaceOrderBodyBuilder
import com.tangem.data.polymarket.converter.PredictionOrderQuoteConverter
import com.tangem.data.polymarket.converter.PredictionPlaceOrderConverter
import com.tangem.data.polymarket.converter.PredictionOrderStateConverter
import com.tangem.data.polymarket.error.PolymarketAuthErrorResolver
import com.tangem.data.polymarket.error.PolymarketEventErrorResolver
import com.tangem.data.polymarket.error.PolymarketWalletErrorResolver
import com.tangem.data.polymarket.error.PredictionPlaceOrderErrorResolver
import com.tangem.data.polymarket.pagination.PolymarketEventsBatchFetcher
import com.tangem.data.polymarket.pagination.PolymarketEventsUpdateFetcher
import com.tangem.data.polymarket.signer.PolymarketL2HeaderBuilder
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobApi
import com.tangem.grow.datasource.polymarket.clob.PolymarketClobPriceFormat
import com.tangem.grow.datasource.polymarket.geo.PolymarketGeoApi
import com.tangem.grow.datasource.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.grow.datasource.polymarket.relayer.PolymarketRelayerApi
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketBalanceAllowance
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketEventError
import com.tangem.domain.polymarket.model.PolymarketEventsBatchFlow
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.model.PolymarketEventsPage
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketSearchBatchFlow
import com.tangem.domain.polymarket.model.PolymarketSearchBatchingContext
import com.tangem.domain.polymarket.model.PolymarketSearchConfig
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.model.PredictionOrderQuote
import com.tangem.domain.polymarket.model.PredictionOrderSide
import com.tangem.domain.polymarket.model.PredictionOrderState
import com.tangem.domain.polymarket.model.PredictionOrderStateError
import com.tangem.domain.polymarket.model.PredictionPriceError
import com.tangem.domain.polymarket.model.PredictionOrderQuoteError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderError
import com.tangem.domain.polymarket.model.PredictionPlaceOrderResult
import com.tangem.domain.polymarket.model.PredictionSignedOrder
import com.tangem.domain.polymarket.model.PredictionOrderQuoteRequest
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.toHexString
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.math.BigDecimal
import java.math.BigInteger
import java.net.HttpURLConnection
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList", "LargeClass")
internal class DefaultPolymarketRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val geoApi: PolymarketGeoApi,
    private val relayerApi: PolymarketRelayerApi,
    private val clobApi: PolymarketClobApi,
    private val walletErrorResolver: PolymarketWalletErrorResolver,
    private val authErrorResolver: PolymarketAuthErrorResolver,
    private val eventErrorResolver: PolymarketEventErrorResolver,
    private val placeOrderErrorResolver: PredictionPlaceOrderErrorResolver,
    private val l2HeaderBuilder: PolymarketL2HeaderBuilder,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketRepository {

    private val builderCodeCache = AtomicReference<String?>(null)

    override suspend fun getCategories(): Either<DataError, List<PolymarketCategory>> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                polymarketApi.getCategories(locale = null).bind().categories
                    .map { PolymarketCategory(id = it.id, label = it.label, iconUrl = it.icon) }
                    .right()
            },
            onError = { DataError.NetworkError.NoInternetConnection.left() },
        )
    }

    override fun getEventsBatchFlow(
        context: PolymarketEventsBatchingContext,
        batchSize: Int,
    ): PolymarketEventsBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = PolymarketEventsBatchFetcher(
                batchSize = batchSize,
                fetchPage = ::fetchEventsPage,
            ),
            updateFetcher = PolymarketEventsUpdateFetcher(
                batchSize = batchSize,
                fetchPage = ::fetchEventsPage,
            ),
        ).toBatchFlow()
    }

    override fun searchEventsBatchFlow(
        context: PolymarketSearchBatchingContext,
        batchSize: Int,
    ): PolymarketSearchBatchFlow {
        return BatchListSource(
            fetchDispatcher = dispatchers.io,
            context = context,
            generateNewKey = { keys -> keys.lastOrNull()?.inc() ?: 0 },
            batchFetcher = LimitOffsetBatchFetcher(
                prefetchDistance = batchSize,
                batchSize = batchSize,
                subFetcher = ::fetchSearchPage,
            ),
        ).toBatchFlow()
    }

    /** The BFF pages search with a 1-based page number; the fetcher speaks offsets, so translate. */
    private suspend fun fetchSearchPage(
        request: LimitOffsetBatchFetcher.Request<PolymarketSearchConfig>,
        @Suppress("UnusedParameter") lastResult: BatchFetchResult<List<PolymarketEvent>>?,
        @Suppress("UnusedParameter") isFirstBatchFetching: Boolean,
    ): BatchFetchResult<List<PolymarketEvent>> {
        val response = polymarketApi.searchEvents(
            query = request.params.query,
            limit = request.limit,
            page = request.offset / request.limit + 1,
        )
        return when (response) {
            is ApiResponse.Success -> BatchFetchResult.Success(
                data = response.data.events.map(PolymarketEventConverter::convert),
                empty = response.data.events.isEmpty(),
                last = !response.data.hasNext,
            )
            is ApiResponse.Error -> throw response.cause
        }
    }

    /** Throws on failure: the pagination turns the throwable into a fetch error of the batch. */
    private suspend fun fetchEventsPage(
        config: PolymarketEventsListConfig,
        cursor: String?,
        limit: Int,
    ): PolymarketEventsPage {
        val response = polymarketApi.getEvents(category = config.category, limit = limit, cursor = cursor)
        return when (response) {
            is ApiResponse.Success -> PolymarketEventsPage(
                events = response.data.events.map(PolymarketEventConverter::convert),
                cursor = response.data.cursor,
                hasNext = response.data.hasNext,
            )
            is ApiResponse.Error -> throw response.cause
        }
    }

    override suspend fun getEvent(eventId: String): Either<PolymarketEventError, PolymarketEvent> =
        withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    PolymarketEventConverter.convert(polymarketApi.getEvent(eventId = eventId).bind().event).right()
                },
                onError = { eventErrorResolver.resolve(it).left() },
            )
        }

    override suspend fun placeOrder(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
        signedOrder: PredictionSignedOrder,
        isNegRisk: Boolean,
    ): Either<PredictionPlaceOrderError, PredictionPlaceOrderResult> = withContext(dispatchers.io) {
        if (!signedOrder.isSignedFor(isNegRisk = isNegRisk)) {
            return@withContext PredictionPlaceOrderError.NotSent(
                detail = "Order signed for the ${if (isNegRisk) "standard" else "neg-risk"} exchange",
            ).left()
        }

        val json = Either
            .catch { PolymarketPlaceOrderBodyBuilder.build(signedOrder = signedOrder, apiKey = credentials.apiKey) }
            .mapLeft { PredictionPlaceOrderError.NotSent(detail = it.message) }
            .getOrElse { return@withContext it.left() }

        val headers = Either
            .catch {
                l2HeaderBuilder.build(
                    ownerAddress = ownerAddress,
                    credentials = credentials,
                    requestPath = PLACE_ORDER_SIGNED_PATH,
                    method = HTTP_METHOD_POST,
                    body = json,
                )
            }
            .mapLeft { PredictionPlaceOrderError.NotSent(detail = it.message) }
            .getOrElse { return@withContext it.left() }

        safeApiCall(
            call = {
                val response = clobApi.placeOrder(
                    headers = headers,
                    body = json.toRequestBody(contentType = JSON_MEDIA_TYPE),
                ).bind()

                Either.catch { PredictionPlaceOrderConverter.convert(value = response) }
                    .mapLeft { PredictionPlaceOrderError.Ambiguous(detail = it.message) }
                    .flatMap { result ->
                        if (result.isSuccess) {
                            result.right()
                        } else {
                            PredictionPlaceOrderError.Rejected(detail = result.errorMessage).left()
                        }
                    }
            },
            onError = { placeOrderErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun getOrder(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
        orderId: String,
    ): Either<PredictionOrderStateError, PredictionOrderState> = withContext(dispatchers.io) {
        val headers = Either
            .catch {
                l2HeaderBuilder.build(
                    ownerAddress = ownerAddress,
                    credentials = credentials,
                    requestPath = "$ORDER_READ_SIGNED_PATH/$orderId",
                )
            }
            .mapLeft { PredictionOrderStateError.Unknown(httpCode = null, detail = it.message) }
            .getOrElse { return@withContext it.left() }

        safeApiCallWithTimeout(
            timeoutMillis = ORDER_READ_TIMEOUT,
            call = {
                val response = clobApi.getOrder(headers = headers, orderId = orderId).bind()
                PredictionOrderStateConverter.convert(value = response).right()
            },
            onError = { it.toOrderStateError().left() },
        )
    }

    override suspend fun getPrice(
        assetId: String,
        side: PredictionOrderSide,
    ): Either<PredictionPriceError, BigDecimal> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = clobApi.getPrice(tokenId = assetId, side = side.name).bind()

                Either.catch { BigDecimal(response.price) }
                    .mapLeft { error ->
                        TangemLogger.e("Unreadable book price", error)
                        PredictionPriceError.Unknown(httpCode = null, detail = error.message)
                    }
                    .flatMap { price ->
                        if (price.compareTo(side.emptyBookSentinel()) == 0) {
                            PredictionPriceError.EmptyBook.left()
                        } else {
                            price.right()
                        }
                    }
            },
            onError = { it.toPriceError().left() },
        )
    }

    private fun PredictionOrderSide.emptyBookSentinel(): BigDecimal = when (this) {
        PredictionOrderSide.BUY -> PolymarketClobPriceFormat.EMPTY_BID_PRICE
        PredictionOrderSide.SELL -> PolymarketClobPriceFormat.EMPTY_ASK_PRICE
    }

    override suspend fun getOrderQuote(
        request: PredictionOrderQuoteRequest,
    ): Either<PredictionOrderQuoteError, PredictionOrderQuote> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = polymarketApi.getOrderQuote(
                    request = PredictionOrderQuoteConverter.toRequestBody(request = request),
                ).bind()

                Either.catch { PredictionOrderQuoteConverter.convert(value = response) }
                    .mapLeft { error ->
                        TangemLogger.e("Unreadable order quote", error)
                        PredictionOrderQuoteError.Unknown(httpCode = null, detail = error.message)
                    }
            },
            onError = { it.toQuoteError().left() },
        )
    }

    override suspend fun getWalletStatus(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletState> =
        withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    PolymarketWalletConverter.toState(polymarketApi.getWalletStatus(ownerAddress).bind()).right()
                },
                onError = { walletErrorResolver.resolve(it).left() },
            )
        }

    override suspend fun getWalletStatusByWalletId(
        walletId: String,
    ): Either<PolymarketWalletError, PolymarketWalletState> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                PolymarketWalletConverter.toState(polymarketApi.getWalletStatusByWalletId(walletId).bind()).right()
            },
            onError = { walletErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun deployWallet(
        ownerAddress: String,
        walletId: String,
        depositWalletAddress: String,
    ): Either<PolymarketWalletError, PolymarketWalletStatus> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = polymarketApi.deployWallet(
                    PolymarketWalletDeployRequest(
                        ownerAddress = ownerAddress,
                        walletId = walletId,
                        depositWalletAddress = depositWalletAddress,
                    ),
                ).bind()
                PolymarketWalletStatus.fromRaw(response.status).right()
            },
            onError = { walletErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun submitApprovals(
        batch: PolymarketApprovalsBatch,
    ): Either<PolymarketWalletError, PolymarketWalletStatus> = withContext(dispatchers.io) {
        safeApiCall(
            call = {
                val response = polymarketApi.submitApprovals(PolymarketWalletConverter.toRequest(batch)).bind()
                PolymarketWalletStatus.fromRaw(response.status).right()
            },
            onError = { walletErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun getBuilderCode(walletId: String): Either<DataError, String> = withContext(dispatchers.io) {
        builderCodeCache.get()?.let { return@withContext it.right() }

        safeApiCall(
            call = {
                polymarketApi.getConfiguration(walletId = walletId).bind().builderCode
                    .also(builderCodeCache::set)
                    .right()
            },
            onError = { DataError.NetworkError.NoInternetConnection.left() },
        )
    }

    override suspend fun checkGeoblock(): Either<DataError, Boolean> = withContext(dispatchers.io) {
        safeApiCall(
            call = { geoApi.getGeoblock().bind().blocked.right() },
            onError = { DataError.NetworkError.NoInternetConnection.left() },
        )
    }

    override suspend fun getRelayerNonce(ownerAddress: String): Either<DataError, BigInteger> =
        withContext(dispatchers.io) {
            safeApiCall(
                call = {
                    relayerApi.getNonce(address = ownerAddress, type = WALLET_NONCE_TYPE).bind().nonce
                        .toBigIntegerOrNull()?.right()
                        ?: DataError.NetworkError.NoInternetConnection.left()
                },
                onError = { DataError.NetworkError.NoInternetConnection.left() },
            )
        }

    override suspend fun deriveApiCredentials(
        headers: PolymarketL1Headers,
    ): Either<PolymarketAuthError, PolymarketApiCredentials> = withContext(dispatchers.io) {
        safeApiCall(
            call = { PolymarketApiKeyConverter.convert(clobApi.deriveApiKey(headers.toMap()).bind()).right() },
            onError = { authErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun createApiCredentials(
        headers: PolymarketL1Headers,
    ): Either<PolymarketAuthError, PolymarketApiCredentials> = withContext(dispatchers.io) {
        safeApiCall(
            call = { PolymarketApiKeyConverter.convert(clobApi.createApiKey(headers.toMap()).bind()).right() },
            onError = { authErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun syncBalanceAllowance(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
    ): Either<PolymarketAuthError, Unit> = withContext(dispatchers.io) {
        val headers = buildL2Headers(
            ownerAddress = ownerAddress,
            credentials = credentials,
            requestPath = BALANCE_ALLOWANCE_SIGNED_PATH,
        ).getOrElse { return@withContext it.left() }

        safeApiCallWithTimeout(
            timeoutMillis = SYNC_BALANCE_ALLOWANCE_TIMEOUT,
            call = {
                clobApi.updateBalanceAllowance(
                    headers = headers,
                    assetType = ASSET_TYPE_COLLATERAL,
                    signatureType = PolymarketContracts.SIGNATURE_TYPE_DEPOSIT_WALLET,
                ).bind().right()
            },
            onError = { authErrorResolver.resolve(it).left() },
        )
    }

    override suspend fun getBalanceAllowance(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
    ): Either<PolymarketAuthError, PolymarketBalanceAllowance> = withContext(dispatchers.io) {
        val headers = buildL2Headers(
            ownerAddress = ownerAddress,
            credentials = credentials,
            requestPath = BALANCE_ALLOWANCE_READ_SIGNED_PATH,
        ).getOrElse { return@withContext it.left() }

        safeApiCallWithTimeout(
            timeoutMillis = SYNC_BALANCE_ALLOWANCE_TIMEOUT,
            call = {
                val response = clobApi.getBalanceAllowance(
                    headers = headers,
                    assetType = ASSET_TYPE_COLLATERAL,
                    signatureType = PolymarketContracts.SIGNATURE_TYPE_DEPOSIT_WALLET,
                ).bind()
                Either.catch { PolymarketBalanceAllowanceConverter.convert(response) }
                    .mapLeft { PolymarketAuthError.Unknown(httpCode = null, detail = it.message) }
            },
            onError = { authErrorResolver.resolve(it).left() },
        )
    }

    /**
     * A malformed stored secret makes the HMAC throw, which must not escape past the call's error boundary
     * into the caller.
     */
    private fun buildL2Headers(
        ownerAddress: String,
        credentials: PolymarketApiCredentials,
        requestPath: String,
    ): Either<PolymarketAuthError, Map<String, String>> = Either
        .catch {
            l2HeaderBuilder.build(
                ownerAddress = ownerAddress,
                credentials = credentials,
                requestPath = requestPath,
            )
        }
        .mapLeft { PolymarketAuthError.Unknown(httpCode = null, detail = it.message) }

    /**
     * A quote is re-requested on a timer, so the caller mostly needs to know whether waiting for the next tick
     * can help. Only the transport failures promise that; an HTTP refusal carries its status so a deterministic
     * one — a rejected request body, say — can be told apart from a server that is merely unwell.
     */
    private fun ApiResponseError.toPriceError(): PredictionPriceError = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PredictionPriceError.Network
        is ApiResponseError.HttpException -> when (code.numericCode) {
            HttpURLConnection.HTTP_NOT_FOUND -> PredictionPriceError.MarketClosed
            HttpURLConnection.HTTP_BAD_REQUEST -> PredictionPriceError.InvalidAsset
            else -> PredictionPriceError.Unknown(httpCode = code.numericCode, detail = errorBody)
        }
        is ApiResponseError.UnknownException ->
            PredictionPriceError.Unknown(httpCode = null, detail = cause.message ?: message)
    }

    private fun ApiResponseError.toOrderStateError(): PredictionOrderStateError = when (this) {
        is ApiResponseError.NetworkException -> PredictionOrderStateError.Network
        is ApiResponseError.TimeoutException -> PredictionOrderStateError.Timeout
        is ApiResponseError.HttpException -> when (code) {
            Code.UNAUTHORIZED -> PredictionOrderStateError.Unauthorized(detail = errorBody)
            Code.NOT_FOUND -> PredictionOrderStateError.NotFound
            else -> PredictionOrderStateError.Unknown(httpCode = code.numericCode, detail = errorBody)
        }
        is ApiResponseError.UnknownException ->
            PredictionOrderStateError.Unknown(httpCode = null, detail = cause.message ?: message)
    }

    private fun ApiResponseError.toQuoteError(): PredictionOrderQuoteError = when (this) {
        is ApiResponseError.NetworkException,
        is ApiResponseError.TimeoutException,
        -> PredictionOrderQuoteError.Network
        is ApiResponseError.HttpException ->
            PredictionOrderQuoteError.Unknown(httpCode = code.numericCode, detail = errorBody)
        is ApiResponseError.UnknownException ->
            PredictionOrderQuoteError.Unknown(httpCode = null, detail = cause.message ?: message)
    }

    private fun PredictionSignedOrder.isSignedFor(isNegRisk: Boolean): Boolean {
        val expected = PolymarketOrderTypedDataBuilder.domainSeparator(isNegRisk = isNegRisk)
            .toHexString()
            .lowercase()
        val tail = signature.removePrefix(PolymarketContracts.HEX_PREFIX)
            .lowercase()
            .drop(n = PolymarketContracts.SIGNATURE_RSV_HEX_LENGTH)

        return tail.startsWith(expected)
    }

    private companion object {
        const val DEFAULT_LIMIT = 20
        const val WALLET_NONCE_TYPE = "WALLET"
        const val ASSET_TYPE_COLLATERAL = "COLLATERAL"

        val SYNC_BALANCE_ALLOWANCE_TIMEOUT = 5.seconds
        val ORDER_READ_TIMEOUT = 5.seconds

        const val HTTP_METHOD_POST = "POST"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()

        const val PLACE_ORDER_SIGNED_PATH = "/order"
        const val ORDER_READ_SIGNED_PATH = "/data/order"

        const val BALANCE_ALLOWANCE_SIGNED_PATH = "/balance-allowance/update"
        const val BALANCE_ALLOWANCE_READ_SIGNED_PATH = "/balance-allowance"
    }
}