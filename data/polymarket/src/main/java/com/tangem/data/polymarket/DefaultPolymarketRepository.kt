package com.tangem.data.polymarket

import arrow.core.Either
import com.tangem.data.polymarket.converter.PolymarketEventConverter
import com.tangem.data.polymarket.converter.PolymarketWalletConverter
import com.tangem.data.polymarket.error.PolymarketWalletErrorResolver
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.polymarket.PolymarketApi
import com.tangem.datasource.api.polymarket.models.PolymarketWalletDeployRequest
import com.tangem.domain.core.error.DataError
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketEvent
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class DefaultPolymarketRepository @Inject constructor(
    private val polymarketApi: PolymarketApi,
    private val eventConverter: PolymarketEventConverter,
    private val walletConverter: PolymarketWalletConverter,
    private val walletErrorResolver: PolymarketWalletErrorResolver,
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
    }
}