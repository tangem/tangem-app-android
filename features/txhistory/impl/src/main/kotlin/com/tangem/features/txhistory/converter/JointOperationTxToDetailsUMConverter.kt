package com.tangem.features.txhistory.converter

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Details of a joint transfer rendered from the operation: not on the loaded chain page, or failed inside its relayer
 * transaction — which still names the fee the Safe refunded.
 */
internal class JointOperationTxToDetailsUMConverter(
    private val currency: CryptoCurrency,
    private val onCopyAddress: (String) -> Unit,
    private val menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM>,
    private val isBalanceHidden: Boolean,
    private val resolveRecipient: (String) -> ResolvedOwner,
    private val fiatConversion: FiatConversion,
) : Converter<JointOperationTx, TxHistoryDetailsUM.SingleAsset> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: JointOperationTx): TxHistoryDetailsUM.SingleAsset {
        val status = value.operation.status
        val recipient = value.transfer.intent.recipient
        val cryptoAmount = value
            .transfer
            .intent
            .amountWei
            .toBigDecimal()
            .movePointLeft(currency.decimals)

        val amount = cryptoAmount.format { crypto(cryptoCurrency = currency, ignoreSymbolPosition = true) }
        val isExecuted = status == FinishedJointOperation.Status.EXECUTED

        return TxHistoryDetailsUM.SingleAsset(
            header = TxHistoryDetailsUM.HeaderUM(
                icon = TxIcon.Vector(Icons.ic_arrow_up_20),
                status = status.toUiStatus(),
                title = status.toTitle(),
                subtitle = headerSubtitle(value.timestampMillis),
                menu = menu.withCopyAddress(recipient, onCopyAddress),
            ),
            isBalanceHidden = isBalanceHidden,
            amountBlock = TxHistoryDetailsUM.AmountBlockUM(
                icon = TxHistoryDetailsUM.AmountIconUM.Single(iconStateConverter.convert(currency)),
                amount = stringReference(
                    if (isExecuted) "${StringsSigns.MINUS_SIGN}${StringsSigns.THIN_SPACE}$amount" else amount,
                ),
                isFailed = !isExecuted,
                fiatAmount = fiatConversion.convert(cryptoAmount),
            ),
            counterparty = resolveRecipient(recipient).toCounterpartyUM(
                label = resourceReference(R.string.send_recipient),
                onCopyClick = { onCopyAddress(recipient) },
            ),
            rows = (value.txInfo as? OnChainTx.BSDK)?.txInfo?.feeRows(currency) ?: persistentListOf(),
        )
    }
}