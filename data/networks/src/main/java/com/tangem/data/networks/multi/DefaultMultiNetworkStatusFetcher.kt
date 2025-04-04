package com.tangem.data.networks.multi

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Default implementation of [MultiNetworkStatusFetcher]
 *
 * @property singleNetworkStatusFetcher single network status fetcher
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiNetworkStatusFetcher @Inject constructor(
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
) : MultiNetworkStatusFetcher {

    override suspend fun invoke(params: MultiNetworkStatusFetcher.Params): Either<Throwable, Unit> = either {
        val result = coroutineScope {
            params.networks
                .map {
                    async {
                        singleNetworkStatusFetcher(
                            params = SingleNetworkStatusFetcher.Params(
                                userWalletId = params.userWalletId,
                                network = it,
                            ),
                        )
                    }
                }
                .awaitAll()
        }

        val failedResult = result.firstOrNull { it.isLeft() }

        ensure(failedResult == null) {
            IllegalStateException("Failed to fetch network statuses")
        }
    }
}