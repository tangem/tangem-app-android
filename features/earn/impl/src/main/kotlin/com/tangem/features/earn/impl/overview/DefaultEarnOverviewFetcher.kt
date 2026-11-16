package com.tangem.features.earn.impl.overview

import arrow.core.Either
import arrow.core.right
import com.tangem.features.earn.api.overview.EarnOverviewFetcher
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

internal class DefaultEarnOverviewFetcher @Inject constructor(
    private val registry: EarnProductsRegistry,
) : EarnOverviewFetcher {

    /** Products refresh in parallel: one failure does not cancel the others, but its `Left` reaches Retry. */
    override suspend fun invoke(params: EarnOverviewFetcher.Params): Either<Throwable, Unit> = coroutineScope {
        val productParams = EarnProduct.Params(params.userWalletId, params.currency)
        val results = registry.products
            .map { product ->
                async {
                    product.refresh(productParams).onLeft {
                        TangemLogger.e("Earn refresh failed: ${product.id.value}", it)
                    }
                }
            }
            .awaitAll()

        results.firstOrNull { it.isLeft() } ?: Unit.right()
    }
}