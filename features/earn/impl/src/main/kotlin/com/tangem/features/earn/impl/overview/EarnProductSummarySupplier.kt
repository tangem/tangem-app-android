package com.tangem.features.earn.impl.overview

import com.tangem.domain.core.flow.FlowCachingSupplier
import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.product.EarnProductSummary
import kotlinx.coroutines.flow.Flow

/**
 * @property products summaries of products in [EarnSummaryOrder]: the bottom sheet, the picker, `bestActive` and the
 * banner (without products that have a legacy block) read them
 * @property rateSources summaries of rate sources (Yield Mode, StakeKit) in the same order: only the headline reads
 * them
 */
internal data class EarnProductSummaries(
    val products: Map<CryptoCurrency.ID, List<EarnProductSummary>>,
    val rateSources: Map<CryptoCurrency.ID, List<EarnProductSummary>>,
    val source: StatusSource,
) {

    operator fun get(id: CryptoCurrency.ID): List<EarnProductSummary> = products[id].orEmpty()
}

internal interface EarnProductSummaryProducer : FlowProducer<EarnProductSummaries> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, EarnProductSummaryProducer>
}

/** Polls the products of a wallet once for all subscribers: the flows are cached by wallet. */
internal class EarnProductSummarySupplier(
    override val factory: EarnProductSummaryProducer.Factory,
    override val keyCreator: (EarnProductSummaryProducer.Params) -> String,
) : FlowCachingSupplier<EarnProductSummaryProducer, EarnProductSummaryProducer.Params, EarnProductSummaries>() {

    operator fun invoke(userWalletId: UserWalletId): Flow<EarnProductSummaries> =
        invoke(EarnProductSummaryProducer.Params(userWalletId))
}