package com.tangem.features.earn.lending.impl.product

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.earn.lending.impl.supplier.LendingAssetState
import com.tangem.features.earn.lending.impl.supplier.LendingSnapshot
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.features.earn.product.api.rate.EarnRateBadgeConverter
import com.tangem.utils.converter.Converter

internal class LendingSummaryConverter(
    private val rateBadgeConverter: EarnRateBadgeConverter,
) : Converter<LendingSnapshot, Map<CryptoCurrency.ID, EarnProductSummary>> {

    override fun convert(value: LendingSnapshot): Map<CryptoCurrency.ID, EarnProductSummary> =
        value.assets.mapNotNull { (id, state) -> toSummary(state)?.let { id to it } }.toMap()

    private fun toSummary(state: LendingAssetState): EarnProductSummary? {
        val rate = state.market.rate ?: return null
        val position = state.market.position
        return EarnProductSummary(
            productId = LendingEarnProduct.ID,
            rateText = rateBadgeConverter.convert(rate),
            risk = EarnRisk.Low,
            availability = state.availability,
            rate = rate,
            position = position?.let { EarnPositionSummary(it.cryptoAmount) },
            source = state.source,
            onClickAction = position?.let { LendingNavigationAction.OpenPosition }
                ?: LendingNavigationAction.OpenPicker,
        )
    }
}