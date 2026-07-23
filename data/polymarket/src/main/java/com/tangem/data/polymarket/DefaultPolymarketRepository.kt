package com.tangem.data.polymarket

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.data.common.api.safeApiCall
import com.tangem.data.polymarket.converter.PolymarketApiKeyConverter
import com.tangem.data.polymarket.converter.PolymarketEventConverter
import com.tangem.data.polymarket.converter.PolymarketWalletConverter
import com.tangem.data.polymarket.error.PolymarketAuthErrorResolver
import com.tangem.data.polymarket.error.PolymarketWalletErrorResolver
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.polymarket.PolymarketApi
import com.tangem.datasource.api.polymarket.clob.PolymarketClobApi
import com.tangem.datasource.api.polymarket.geo.PolymarketGeoApi
import com.tangem.datasource.api.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.datasource.api.polymarket.relayer.PolymarketRelayerApi
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketL1Headers
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.math.BigInteger
import javax.inject.Inject

@Suppress("LongParameterList")
internal class DefaultPolymarketRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val geoApi: PolymarketGeoApi,
    private val relayerApi: PolymarketRelayerApi,
    private val clobApi: PolymarketClobApi,
    private val eventConverter: PolymarketEventConverter,
    private val walletConverter: PolymarketWalletConverter,
    private val walletErrorResolver: PolymarketWalletErrorResolver,
    private val authErrorResolver: PolymarketAuthErrorResolver,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketRepository {

    override suspend fun getEvents(): Either<DataError, List<PolymarketEvent>> = withContext(dispatchers.io) {
        Either.catch {
            polymarketApi.getEvents(limit = DEFAULT_LIMIT, cursor = null)
                .getOrThrow()
                .events
                .map(eventConverter::convert)
        }.mapLeft {
            // DataError exposes only NoInternetConnection as a network failure.
            DataError.NetworkError.NoInternetConnection
        }
    }

    override suspend fun getWalletStatus(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletState> =
        withContext(dispatchers.io) {
            Either.catch {
                walletConverter.toState(polymarketApi.getWalletStatus(ownerAddress).getOrThrow())
            }.mapLeft(::resolveError)
        }

    override suspend fun deployWallet(ownerAddress: String): Either<PolymarketWalletError, PolymarketWalletStatus> =
        withContext(dispatchers.io) {
            Either.catch {
                val response = polymarketApi.deployWallet(
                    PolymarketWalletDeployRequest(ownerAddress = ownerAddress),
                ).getOrThrow()
                PolymarketWalletStatus.fromRaw(response.status)
            }.mapLeft(::resolveError)
        }

    override suspend fun submitApprovals(
        batch: PolymarketApprovalsBatch,
    ): Either<PolymarketWalletError, PolymarketWalletStatus> = withContext(dispatchers.io) {
        Either.catch {
            val response = polymarketApi.submitApprovals(walletConverter.toRequest(batch)).getOrThrow()
            PolymarketWalletStatus.fromRaw(response.status)
        }.mapLeft(::resolveError)
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

    /**
     * Re-throws [CancellationException] so coroutine cancellation isn't swallowed by `Either.catch` and
     * turned into a domain error, then maps any real failure to a typed [PolymarketWalletError].
     */
    private fun resolveError(throwable: Throwable): PolymarketWalletError {
        if (throwable is CancellationException) throw throwable
        return walletErrorResolver.resolve(throwable)
    }

    private companion object {

        const val DEFAULT_LIMIT = 20
        const val WALLET_NONCE_TYPE = "WALLET"
    }
}