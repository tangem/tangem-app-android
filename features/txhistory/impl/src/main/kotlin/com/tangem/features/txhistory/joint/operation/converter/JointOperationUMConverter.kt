package com.tangem.features.txhistory.joint.operation.converter

import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.cosign.CosignRequestRowUM
import com.tangem.common.ui.cosign.SignatureProgressUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.joint.operation.model.JointOperationState
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.toBriefAddressFormat
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

/** The operation laid out as the single-asset transaction details it becomes once sent */
internal class JointOperationUMConverter(
    private val currency: CryptoCurrency,
    private val onSignClick: () -> Unit,
    private val onSignersClick: () -> Unit,
    private val onCopyRecipientClick: () -> Unit,
) : Converter<JointOperationUMConverter.Input, TxHistoryDetailsUM.SingleAsset> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: Input): TxHistoryDetailsUM.SingleAsset {
        val state = value.state

        return TxHistoryDetailsUM.SingleAsset(
            header = header(),
            isBalanceHidden = value.isBalanceHidden,
            amountBlock = TxHistoryDetailsUM.AmountBlockUM(
                icon = TxHistoryDetailsUM.AmountIconUM.Single(iconStateConverter.convert(currency)),
                amount = stringReference(amountOf(state)),
                isFailed = false,
            ),
            counterparty = TxHistoryDetailsUM.CounterpartyUM(
                label = resourceReference(R.string.send_recipient),
                title = stringReference(state.displayed.recipient.toBriefAddressFormat()),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Address(rawAddress = state.displayed.recipient),
                onCopyClick = onCopyRecipientClick,
            ),
            rows = listOfNotNull(feeRow(state)).toImmutableList(),
            cosign = CosignRequestRowUM(
                title = resourceReference(
                    id = CoreUiR.string.joint_operation_signatures_progress,
                    formatArgs = wrappedList(state.collected, state.membersCount),
                ),
                progress = SignatureProgressUM(collected = state.collected, total = state.membersCount),
                onInfoClick = onSignersClick,
            ),
            cosignAction = action(state = state, isSigning = value.isSigning, hasAnswered = value.hasAnswered),
        )
    }

    /** Neutral icon and title by design: the warning-coloured subtitle alone says the operation is pending */
    private fun header() = TxHistoryDetailsUM.HeaderUM(
        icon = TxIcon.Vector(Icons.ic_arrow_up_20),
        status = TransactionItemUM.Content.Status.Confirmed,
        title = resourceReference(R.string.common_send),
        subtitle = resourceReference(CoreUiR.string.transaction_history_pending),
        subtitleStyle = TxHistoryDetailsUM.HeaderUM.SubtitleStyle.Warning,
    )

    private fun action(
        state: JointOperationState,
        isSigning: Boolean,
        hasAnswered: Boolean,
    ): TxHistoryDetailsUM.CosignActionUM {
        val canSign = state.canSign && !hasAnswered

        val (buttonText, caption) = when {
            state.ownAnswer == Answer.DECLINED ->
                CoreUiR.string.joint_operation_declined to CoreUiR.string.joint_operation_declined_footer
            hasAnswered || state.ownAnswer == Answer.SIGNED || state.ownAnswer == Answer.CREATED ->
                CoreUiR.string.joint_operation_signed to CoreUiR.string.joint_operation_signed_footer
            else -> CoreUiR.string.common_sign to CoreUiR.string.joint_operation_auto_sends_footer
        }

        return TxHistoryDetailsUM.CosignActionUM(
            buttonText = resourceReference(buttonText),
            caption = resourceReference(
                id = caption,
                formatArgs = wrappedList(state.threshold, state.membersCount),
            ),
            isEnabled = canSign,
            isLoading = isSigning,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_20).takeIf { canSign },
            onClick = onSignClick,
        )
    }

    private fun amountOf(state: JointOperationState): String {
        val formatted = state.displayed.amountWei
            .toBigDecimal()
            .movePointLeft(currency.decimals)
            .format { crypto(cryptoCurrency = currency, ignoreSymbolPosition = true) }

        return "${StringsSigns.MINUS} $formatted"
    }

    private fun feeRow(state: JointOperationState): TxHistoryDetailsUM.InfoRowUM? {
        val coin = state.feeCoin ?: return null
        val fee = state.feeWei.toBigDecimal().movePointLeft(coin.decimals)

        return TxHistoryDetailsUM.InfoRowUM(
            label = resourceReference(R.string.common_network_fee_title),
            value = stringReference(fee.format { crypto(cryptoCurrency = coin, ignoreSymbolPosition = true) }),
            isValueHideable = true,
        )
    }

    data class Input(
        val state: JointOperationState,
        val isSigning: Boolean,
        val hasAnswered: Boolean,
        val isBalanceHidden: Boolean,
    )
}