package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.common.api.safeApiCallWithTimeout
import com.tangem.data.polymarket.converter.PolymarketApiKeyConverter
import com.tangem.data.polymarket.converter.PolymarketBalanceAllowanceConverter
import com.tangem.data.polymarket.converter.PolymarketEventConverter
import com.tangem.data.polymarket.converter.PolymarketWalletConverter
import com.tangem.data.polymarket.error.PolymarketAuthErrorResolver
import com.tangem.data.polymarket.error.PolymarketEventErrorResolver
import com.tangem.data.polymarket.error.PolymarketWalletErrorResolver
import com.tangem.data.polymarket.pagination.PolymarketEventsBatchFetcher
import com.tangem.data.polymarket.signer.PolymarketL2HeaderBuilder
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.polymarket.PolymarketApi
import com.tangem.datasource.api.polymarket.clob.PolymarketClobApi
import com.tangem.datasource.api.polymarket.geo.PolymarketGeoApi
import com.tangem.datasource.api.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.datasource.api.polymarket.relayer.PolymarketRelayerApi
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
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
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListSource
import com.tangem.pagination.fetcher.LimitOffsetBatchFetcher
import com.tangem.pagination.toBatchFlow
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Suppress("LongParameterList")
internal class DefaultPolymarketRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val geoApi: PolymarketGeoApi,
    private val relayerApi: PolymarketRelayerApi,
    private val clobApi: PolymarketClobApi,
    private val walletErrorResolver: PolymarketWalletErrorResolver,
    private val authErrorResolver: PolymarketAuthErrorResolver,
    private val eventErrorResolver: PolymarketEventErrorResolver,
    private val l2HeaderBuilder: PolymarketL2HeaderBuilder,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketRepository {

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
                    signatureType = SIGNATURE_TYPE_DEPOSIT_WALLET,
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
                    signatureType = SIGNATURE_TYPE_DEPOSIT_WALLET,
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

    private companion object {

        const val DEFAULT_LIMIT = 20
        const val WALLET_NONCE_TYPE = "WALLET"
        const val ASSET_TYPE_COLLATERAL = "COLLATERAL"

        /** Polymarket's signature type for a contract-owned deposit wallet (ERC-1271 verification), not a plain EOA. */
        const val SIGNATURE_TYPE_DEPOSIT_WALLET = 3

        val SYNC_BALANCE_ALLOWANCE_TIMEOUT = 5.seconds

        /** Both are signed by the HMAC without the query string, unlike the relative paths Retrofit resolves. */
        const val BALANCE_ALLOWANCE_SIGNED_PATH = "/balance-allowance/update"
        const val BALANCE_ALLOWANCE_READ_SIGNED_PATH = "/balance-allowance"
    }
}