package com.tangem.features.earn.lending.impl.product

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.lending.impl.supplier.LendingSnapshot
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.features.earn.product.api.rate.EarnRateBadgeConverter

internal fun LendingSnapshot.toSummaries(
    rateBadgeConverter: EarnRateBadgeConverter,
): Map<CryptoCurrency.ID, EarnProductSummary> =
    assets.mapNotNull { (id, state) -> state.toSummary(rateBadgeConverter)?.let { id to it } }.toMap()

private fun LendingAssetState.toSummary(rateBadgeConverter: EarnRateBadgeConverter): EarnProductSummary? {
    val rate = market.rate ?: return null
    val position = market.position
    return EarnProductSummary(
        productId = LendingEarnProduct.ID,
        rateText = rateBadgeConverter.convert(rate),
        risk = EarnRisk.Low,
        availability = availability,
        rate = rate,
        position = position?.let { EarnPositionSummary(it.cryptoAmount) },
        source = source,
        onClickAction = position?.let { LendingNavigationAction.OpenPosition } ?: LendingNavigationAction.OpenPicker,
    )
}