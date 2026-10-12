package com.tangem.features.txhistory.joint.operation.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.features.txhistory.entity.TxHistoryDetailsUM
import com.tangem.features.txhistory.joint.operation.signers.ui.state.JointOperationSignersUM

@Immutable
internal sealed interface JointOperationUM {

    data object Loading : JointOperationUM

    data class Content(
        val operation: TxHistoryDetailsUM.SingleAsset,
        val signers: JointOperationSignersUM,
    ) : JointOperationUM

    /** Executed or cancelled by someone else: nothing left to show or sign */
    data object Gone : JointOperationUM
}