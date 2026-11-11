package com.tangem.features.earn.lending.impl.supplier

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.features.earn.lending.impl.provider.LendingProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

internal interface LendingFetcher : FlowFetcher<LendingFetcher.Params> {

    data class Params(val userWalletId: UserWalletId, val currency: CryptoCurrency)
}

/** Refreshes the stores the snapshot listens to; it never writes the snapshot itself */
internal class DefaultLendingFetcher @Inject constructor(
    private val provider: LendingProvider,
    private val networkStatusFetcher: SingleNetworkStatusFetcher,
) : LendingFetcher {

    override suspend fun invoke(params: LendingFetcher.Params): Either<Throwable, Unit> {
        val asset = provider.resolveAsset(params.currency)
            ?: return IllegalArgumentException("Lending is not offered for ${params.currency.id}").left()

        return coroutineScope {
            val providerRefresh = async { provider.refresh(params.userWalletId, asset) }
            val networkRefresh = async {
                networkStatusFetcher(SingleNetworkStatusFetcher.Params(params.userWalletId, asset.currency.network))
            }

            either {
                providerRefresh.await().bind()
                networkRefresh.await().bind()
            }
        }
    }
}