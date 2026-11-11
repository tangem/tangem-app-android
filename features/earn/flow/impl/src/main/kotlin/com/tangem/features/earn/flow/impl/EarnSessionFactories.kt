package com.tangem.features.earn.flow.impl

import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.session.EarnSession
import javax.inject.Inject

/** Session factories of the products that run the shared flow, keyed by [EarnProductId.value]. */
internal class EarnSessionFactories @Inject constructor(
    private val factories: Map<String, @JvmSuppressWildcards EarnSession.Factory>,
) {

    fun find(productId: EarnProductId): EarnSession.Factory? = factories[productId.value]
}