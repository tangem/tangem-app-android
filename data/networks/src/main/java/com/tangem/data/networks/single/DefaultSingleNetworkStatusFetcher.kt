package com.tangem.data.networks.single

import arrow.core.Either
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import javax.inject.Inject

/**
 * Default implementation of [SingleNetworkStatusFetcher]
 *
 * @property multiNetworkStatusFetcher multi network status fetcher
 *
[REDACTED_AUTHOR]
 */
internal class DefaultSingleNetworkStatusFetcher @Inject constructor(
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
) : SingleNetworkStatusFetcher {

    override suspend fun invoke(params: SingleNetworkStatusFetcher.Params): Either<Throwable, Unit> {
        return multiNetworkStatusFetcher(
            params = MultiNetworkStatusFetcher.Params(
                userWalletId = params.userWalletId,
                networks = setOf(params.network),
            ),
        )
    }
}