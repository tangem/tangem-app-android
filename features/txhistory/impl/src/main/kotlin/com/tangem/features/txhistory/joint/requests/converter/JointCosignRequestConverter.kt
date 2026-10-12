package com.tangem.features.txhistory.joint.requests.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.utils.toTimeFormat
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.joint.requests.model.JointCosignRequest
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import java.time.Instant

internal class JointCosignRequestConverter(
    private val currency: CryptoCurrency,
    private val onClick: (operationId: String) -> Unit,
) : Converter<JointCosignRequest, TransactionItemUM> {

    override fun convert(value: JointCosignRequest): TransactionItemUM {
        val operationId = value.operation.operationId
        val recipient = value.transfer.intent.recipient
        val startedAt = value.operation.startedAtMillis()
        val isPendingSignature = value.operation.status == JointOperation.Status.AWAITING_SIGNATURES

        return TransactionItemUM.Content(
            txHash = operationId,
            amount = StringsSigns.MINUS + value.transfer.intent.amountWei
                .toBigDecimal()
                .movePointLeft(currency.decimals)
                .format { crypto(symbol = "", decimals = currency.displayDecimals) }
                .trim(),
            currencySymbol = currency.symbol,
            time = startedAt?.toTimeFormat().orEmpty(),
            status = if (isPendingSignature) {
                TransactionItemUM.Content.Status.PendingSignature
            } else {
                TransactionItemUM.Content.Status.Unconfirmed
            },
            direction = TransactionItemUM.Content.Direction.OUTGOING,
            icon = TxIcon.Vector(Icons.ic_arrow_up_20),
            title = resourceReference(R.string.common_sending),
            subtitle = ContentSubtitle.ExternalAddress(
                direction = ContentSubtitle.Direction.TO,
                rawAddress = recipient,
            ),
            timestamp = startedAt ?: 0L,
            isPendingSignature = isPendingSignature,
            onClick = { onClick(operationId) },
        )
    }

    /**
     * The initiator signs on creation, so their vote doubles as the start time where the backend omits it; without
     * either the row shows no time rather than a clock that would move on every refresh
     */
    private fun JointOperation.startedAtMillis(): Long? {
        val startedAt = createdAt
            ?: signatures.firstOrNull { it.address.equals(initiator, ignoreCase = true) }?.at
            ?: return null

        return runCatching { Instant.parse(startedAt).toEpochMilli() }.getOrNull()
    }
}