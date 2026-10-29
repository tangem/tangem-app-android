package com.tangem.features.txhistory.converter

import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.explorerHash
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.resolveCounterparty
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter

/** A finished joint transfer: the amount and recipient come from the Safe tx, the outcome from the backend status */
internal class JointOperationTxToTransactionItemUMConverter(
    private val currency: CryptoCurrency,
    private val txHistoryUiActions: TxHistoryUiActions,
    private val lookup: TxHistoryLookupContext,
) : Converter<JointOperationTx, TransactionItemUM.Content> {

    override fun convert(value: JointOperationTx): TransactionItemUM.Content {
        val status = value.operation.status
        val prefix = if (status == FinishedJointOperation.Status.EXECUTED) StringsSigns.MINUS else ""

        return TransactionItemUM.Content(
            txHash = value.explorerHash ?: value.txId,
            amount = prefix + value.transfer.intent.amountWei
                .toBigDecimal()
                .movePointLeft(currency.decimals)
                .format { crypto(symbol = "", decimals = currency.displayDecimals) }
                .trim(),
            currencySymbol = currency.symbol,
            status = status.toUiStatus(),
            icon = TxIcon.Vector(Icons.ic_arrow_up_20),
            title = status.toTitle(),
            subtitle = lookup
                .resolveCounterparty(
                    address = value.transfer.intent.recipient,
                    networkRawId = currency.network.id.rawId,
                )
                .toSubtitle(ContentSubtitle.Direction.TO),
            onClick = { txHistoryUiActions.onTransactionClick(value) },
        )
    }
}