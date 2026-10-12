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
import com.tangem.core.ui.res.generated.icons.ic_copy_24
import com.tangem.core.ui.res.generated.icons.ic_cross_24
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.core.ui.res.generated.icons.ic_trash_bin_24
import com.tangem.core.ui.res.generated.icons.ic_user_plus_24
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.joint.operation.model.JointOperationState
import com.tangem.features.txhistory.joint.operation.model.JointOperationState.Role
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

/** The operation laid out as the single-asset transaction details it becomes once sent */
internal class JointOperationUMConverter(
    private val currency: CryptoCurrency,
    private val onSignClick: () -> Unit,
    private val onSignersClick: () -> Unit,
    private val onCopyRecipientClick: () -> Unit,
    private val onCreateContactClick: () -> Unit,
    private val onRefuseClick: () -> Unit,
) : Converter<JointOperationUMConverter.Input, TxHistoryDetailsUM.SingleAsset> {

    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: Input): TxHistoryDetailsUM.SingleAsset {
        val state = value.state

        return TxHistoryDetailsUM.SingleAsset(
            header = header(state = state, menu = menu(state = state, canCreateContact = value.canCreateContact)),
            isBalanceHidden = value.isBalanceHidden,
            amountBlock = TxHistoryDetailsUM.AmountBlockUM(
                icon = TxHistoryDetailsUM.AmountIconUM.Single(iconStateConverter.convert(currency)),
                amount = stringReference(amountOf(state)),
                isFailed = false,
            ),
            counterparty = TxHistoryDetailsUM.CounterpartyUM(
                label = resourceReference(R.string.send_recipient),
                title = TxHistoryDetailsUM.CounterpartyTitle.Address(state.displayed.recipient),
                avatar = TxHistoryDetailsUM.CounterpartyAvatar.Address(rawAddress = state.displayed.recipient),
                onCopyClick = onCopyRecipientClick,
            ),
            rows = listOfNotNull(feeRow(state)).toImmutableList(),
            cosign = if (state.isSending) null else cosignRow(state),
            cosignAction = action(
                state = state,
                isSigning = value.isSigning,
                isRefusing = value.isRefusing,
                hasAnswered = value.hasAnswered,
                isColdWallet = value.isColdWallet,
            ),
        )
    }

    /**
     * While signatures are collected the icon and title stay neutral by design, the warning-coloured subtitle alone
     * says the operation is pending; once sent it reads like any unconfirmed transaction.
     */
    private fun header(
        state: JointOperationState,
        menu: ImmutableList<TxHistoryDetailsUM.MenuItemUM>,
    ): TxHistoryDetailsUM.HeaderUM {
        return if (state.isSending) {
            TxHistoryDetailsUM.HeaderUM(
                icon = TxIcon.Vector(Icons.ic_arrow_up_20),
                status = TransactionItemUM.Content.Status.Unconfirmed,
                title = resourceReference(CoreUiR.string.common_sending),
                subtitle = resourceReference(CoreUiR.string.joint_operation_sending_to_network),
                menu = menu,
            )
        } else {
            TxHistoryDetailsUM.HeaderUM(
                icon = TxIcon.Vector(Icons.ic_arrow_up_20),
                status = TransactionItemUM.Content.Status.Confirmed,
                title = resourceReference(R.string.common_send),
                subtitle = resourceReference(CoreUiR.string.transaction_history_pending),
                subtitleStyle = TxHistoryDetailsUM.HeaderUM.SubtitleStyle.Warning,
                menu = menu,
            )
        }
    }

    private fun cosignRow(state: JointOperationState) = CosignRequestRowUM(
        title = resourceReference(
            id = CoreUiR.string.joint_operation_signatures_progress,
            formatArgs = wrappedList(state.collected, state.membersCount),
        ),
        progress = SignatureProgressUM(collected = state.collected, total = state.membersCount),
        onInfoClick = onSignersClick,
    )

    private fun menu(state: JointOperationState, canCreateContact: Boolean) = buildList {
        add(
            TxHistoryDetailsUM.MenuItemUM(
                icon = Icons.ic_copy_24,
                title = resourceReference(CoreUiR.string.common_copy_address),
                action = TxHistoryDetailsUM.MenuItemUM.Action.Direct(onCopyRecipientClick),
            ),
        )
        if (canCreateContact) {
            add(
                TxHistoryDetailsUM.MenuItemUM(
                    icon = Icons.ic_user_plus_24,
                    title = resourceReference(CoreUiR.string.address_book_add_contact),
                    action = TxHistoryDetailsUM.MenuItemUM.Action.Direct(onCreateContactClick),
                ),
            )
        }
        if (state.isSending || !state.canRefuse) return@buildList
        val isInitiator = state.role == Role.INITIATOR
        add(
            TxHistoryDetailsUM.MenuItemUM(
                icon = if (isInitiator) Icons.ic_trash_bin_24 else Icons.ic_cross_24,
                title = resourceReference(
                    if (isInitiator) CoreUiR.string.common_delete else CoreUiR.string.common_decline,
                ),
                isDestructive = true,
                action = TxHistoryDetailsUM.MenuItemUM.Action.Direct(onRefuseClick),
            ),
        )
    }.toImmutableList()

    private fun action(
        state: JointOperationState,
        isSigning: Boolean,
        isRefusing: Boolean,
        hasAnswered: Boolean,
        isColdWallet: Boolean,
    ): TxHistoryDetailsUM.CosignActionUM {
        // The model overlays a local answer into the state, so the answer alone says whether this member signed;
        // [hasAnswered] also covers a local decline and only blocks a second tap
        val hasSigned = state.ownAnswer == Answer.SIGNED || state.ownAnswer == Answer.CREATED
        if (state.isSending) return sentAction(state = state, hasSigned = hasSigned)

        val canSign = state.canSign && !hasAnswered && !isRefusing

        val (buttonText, caption) = when {
            state.ownAnswer == Answer.DECLINED ->
                CoreUiR.string.joint_operation_declined to CoreUiR.string.joint_operation_declined_footer
            hasSigned -> CoreUiR.string.joint_operation_signed to CoreUiR.string.joint_operation_signed_footer
            else -> CoreUiR.string.common_sign to CoreUiR.string.joint_operation_auto_sends_footer
        }

        return TxHistoryDetailsUM.CosignActionUM(
            buttonText = resourceReference(buttonText),
            caption = resourceReference(
                id = caption,
                formatArgs = wrappedList(state.threshold, state.membersCount),
            ),
            isEnabled = canSign,
            isLoading = isSigning || isRefusing,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_20).takeIf { canSign && isColdWallet },
            onClick = onSignClick,
        )
    }

    /** Nothing left to do once the threshold is reached: the button only records whether this member signed */
    private fun sentAction(state: JointOperationState, hasSigned: Boolean): TxHistoryDetailsUM.CosignActionUM {
        val buttonText = if (hasSigned) CoreUiR.string.joint_operation_signed else CoreUiR.string.common_sign

        return TxHistoryDetailsUM.CosignActionUM(
            buttonText = resourceReference(buttonText),
            caption = resourceReference(
                id = CoreUiR.string.joint_operation_sent_footer,
                formatArgs = wrappedList(state.collected, state.membersCount),
            ),
            isEnabled = false,
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
        val isRefusing: Boolean,
        val hasAnswered: Boolean,
        val canCreateContact: Boolean,
        val isBalanceHidden: Boolean,
        val isColdWallet: Boolean,
    )
}