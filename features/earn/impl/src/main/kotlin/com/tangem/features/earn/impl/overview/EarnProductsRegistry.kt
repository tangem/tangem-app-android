package com.tangem.features.earn.impl.overview

import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnRateSource
import javax.inject.Inject

internal class EarnProductsRegistry @Inject constructor(
    products: Set<@JvmSuppressWildcards EarnProduct>,
    rateSources: Set<@JvmSuppressWildcards EarnRateSource>,
) {

    val products: List<EarnProduct> = products.sortedBy { it.rank }

    val rateSources: List<EarnRateSource> = rateSources.sortedBy { it.rank }

    init {
        val ids = products.map { it.id } + rateSources.map { it.id }
        require(ids.size == ids.toSet().size) { "Duplicate earn product ids: $ids" }
    }

    fun find(id: EarnProductId): EarnProduct? = products.firstOrNull { it.id == id }

    /** Lower is higher: a product with the lower rank wins when rates are equal. */
    fun rankOf(id: EarnProductId): Int = products.firstOrNull { it.id == id }?.rank
        ?: rateSources.firstOrNull { it.id == id }?.rank
        ?: Int.MAX_VALUE
}