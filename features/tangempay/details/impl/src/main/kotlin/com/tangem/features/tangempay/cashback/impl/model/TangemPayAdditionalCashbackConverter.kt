package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayAdditionalCashbackUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayAdditionalCashbackConverter(
    private val dateFormatter: TangemPayCashbackDateFormatter = TangemPayCashbackDateFormatter(),
) : Converter<List<CashbackPromotions.AdditionalCashback>, TangemPayAdditionalCashbackUM> {

    override fun convert(value: List<CashbackPromotions.AdditionalCashback>): TangemPayAdditionalCashbackUM {
        return TangemPayAdditionalCashbackUM(
            items = value.map { promo ->
                TangemPayAdditionalCashbackUM.Item(
                    id = promo.id,
                    name = stringReference(promo.name),
                    description = stringReference(promo.description),
                    badge = promo.toBadge(),
                )
            }.toImmutableList(),
        )
    }

    private fun CashbackPromotions.AdditionalCashback.toBadge(): TangemPayAdditionalCashbackUM.Badge {
        val expiry = endDate
        return if (isPermanent || expiry == null) {
            TangemPayAdditionalCashbackUM.Badge.Permanent
        } else {
            TangemPayAdditionalCashbackUM.Badge.Until(
                text = resourceReference(
                    id = R.string.tangempay_cashback_additional_until,
                    formatArgs = wrappedList(dateFormatter.formatNumericDate(expiry)),
                ),
            )
        }
    }
}