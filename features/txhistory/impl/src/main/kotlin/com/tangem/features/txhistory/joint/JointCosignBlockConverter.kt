package com.tangem.features.txhistory.joint

import com.tangem.common.ui.cosign.CosignRequestRowUM
import com.tangem.common.ui.cosign.SignatureProgressUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

internal class JointCosignBlockConverter(
    private val onSignClick: () -> Unit,
) : Converter<JointCosignBlockConverter.Input, TxHistoryDetailsUM?> {

    override fun convert(value: Input): TxHistoryDetailsUM? {
        val state = value.state
        val cosign = value.cosign
        if (state !is TxHistoryDetailsUM.SingleAsset || cosign == null) return state

        return state.copy(
            header = state.header.copy(
                subtitle = resourceReference(CoreUiR.string.transaction_history_pending),
                subtitleStyle = TxHistoryDetailsUM.HeaderUM.SubtitleStyle.Warning,
                menu = persistentListOf(),
            ),
            cosign = CosignRequestRowUM(
                title = resourceReference(
                    id = CoreUiR.string.joint_operation_signatures_progress,
                    formatArgs = wrappedList(cosign.collected, cosign.membersCount),
                ),
                progress = SignatureProgressUM(collected = cosign.collected, total = cosign.membersCount),
            ),
            cosignAction = cosignAction(
                cosign = cosign,
                isSigning = value.isSigning,
                hasAnswered = value.hasAnswered,
            ),
        )
    }

    private fun cosignAction(
        cosign: JointCosignState,
        isSigning: Boolean,
        hasAnswered: Boolean,
    ): TxHistoryDetailsUM.CosignActionUM {
        val canSign = cosign.canSign && !hasAnswered

        val (buttonText, caption) = when {
            cosign.ownAnswer == Answer.DECLINED ->
                CoreUiR.string.joint_operation_declined to CoreUiR.string.joint_operation_declined_footer
            hasAnswered || cosign.ownAnswer == Answer.SIGNED || cosign.ownAnswer == Answer.CREATED ->
                CoreUiR.string.joint_operation_signed to CoreUiR.string.joint_operation_signed_footer
            else -> CoreUiR.string.common_sign to CoreUiR.string.joint_operation_auto_sends_footer
        }

        return TxHistoryDetailsUM.CosignActionUM(
            buttonText = resourceReference(buttonText),
            caption = resourceReference(
                id = caption,
                formatArgs = wrappedList(cosign.threshold, cosign.membersCount),
            ),
            isEnabled = canSign,
            isLoading = isSigning,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_20).takeIf { canSign },
            onClick = onSignClick,
        )
    }

    data class Input(
        val state: TxHistoryDetailsUM?,
        val cosign: JointCosignState?,
        val isSigning: Boolean,
        val hasAnswered: Boolean = false,
    )
}