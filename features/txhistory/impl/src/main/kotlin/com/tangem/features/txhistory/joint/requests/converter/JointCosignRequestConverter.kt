package com.tangem.features.txhistory.joint.requests.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.txhistory.converter.toSubtitle
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.joint.requests.model.JointCosignRequest
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter

internal class JointCosignRequestConverter(
    private val currency: CryptoCurrency,
    private val onClick: (operationId: String) -> Unit,
) : Converter<JointCosignRequest, TransactionItemUM> {

    override fun convert(value: JointCosignRequest): TransactionItemUM {
        val operationId = value.operation.operationId
        val isPendingSignature = value.operation.status == JointOperation.Status.AWAITING_SIGNATURES

        return TransactionItemUM.Content(
            txHash = operationId,
            amount = StringsSigns.MINUS + value.transfer.intent.amountWei
                .toBigDecimal()
                .movePointLeft(currency.decimals)
                .format { crypto(symbol = "", decimals = currency.displayDecimals) }
                .trim(),
            currencySymbol = currency.symbol,
            status = if (isPendingSignature) {
                TransactionItemUM.Content.Status.PendingSignature
            } else {
                TransactionItemUM.Content.Status.Unconfirmed
            },
            icon = TxIcon.Vector(Icons.ic_arrow_up_20),
            title = resourceReference(R.string.common_sending),
            subtitle = value.recipient.toSubtitle(direction = ContentSubtitle.Direction.TO),
            onClick = { onClick(operationId) },
        )
    }
}