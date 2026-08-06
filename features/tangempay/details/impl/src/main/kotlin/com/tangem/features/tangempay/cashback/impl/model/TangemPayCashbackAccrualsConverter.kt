package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackAccrualsUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayCashbackAccrualsConverter(
    private val onDocClick: (url: String) -> Unit,
) : Converter<List<CashbackDocument>, TangemPayCashbackAccrualsUM> {

    override fun convert(value: List<CashbackDocument>): TangemPayCashbackAccrualsUM {
        return TangemPayCashbackAccrualsUM(
            title = resourceReference(R.string.tangempay_cashback_accruals_title),
            infoRows = INFO_ROWS,
            docRows = value.map { doc ->
                TangemPayCashbackAccrualsUM.DocRow(
                    title = stringReference(doc.title),
                    onClick = { onDocClick(doc.url) },
                )
            }.toImmutableList(),
        )
    }

    private companion object {
        val INFO_ROWS = persistentListOf(
            TangemPayCashbackAccrualsUM.InfoRow(
                title = resourceReference(R.string.tangempay_cashback_accruals_calc_title),
                description = resourceReference(R.string.tangempay_cashback_accruals_calc_description),
            ),
            TangemPayCashbackAccrualsUM.InfoRow(
                title = resourceReference(R.string.tangempay_cashback_accruals_pay_title),
                description = resourceReference(R.string.tangempay_cashback_accruals_pay_description),
            ),
            TangemPayCashbackAccrualsUM.InfoRow(
                title = resourceReference(R.string.tangempay_cashback_accruals_exceptions_title),
                description = resourceReference(R.string.tangempay_cashback_accruals_exceptions_description),
            ),
        )
    }
}