package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackAccrualsUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal class TangemPayCashbackAccrualsConverter(
    private val onDocClick: (url: String) -> Unit,
) : Converter<List<CashbackDocument>, TangemPayCashbackAccrualsUM> {

    // TODO([REDACTED_TASK_KEY]): move hardcoded strings to string resources
    override fun convert(value: List<CashbackDocument>): TangemPayCashbackAccrualsUM {
        return TangemPayCashbackAccrualsUM(
            title = stringReference("Accruals"),
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
                title = stringReference("How we calculate cashback?"),
                description = stringReference(
                    "We process purchases within 5 days after the operation and count only completed transactions",
                ),
            ),
            TangemPayCashbackAccrualsUM.InfoRow(
                title = stringReference("How we pay cashback?"),
                description = stringReference("From the 2nd and the 5th of the next month"),
            ),
            TangemPayCashbackAccrualsUM.InfoRow(
                title = stringReference("Exceptions"),
                description = stringReference(
                    "No cashback will be awarded for in-person/in-store purchases at EU merchants; also for " +
                        "withdrawals, transfers, quasi-cash, mobile phone bills, government services and certain " +
                        "other categories",
                ),
            ),
        )
    }
}