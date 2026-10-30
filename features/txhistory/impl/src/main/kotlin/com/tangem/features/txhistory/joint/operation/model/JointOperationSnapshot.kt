package com.tangem.features.txhistory.joint.operation.model

import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.features.txhistory.joint.operation.signers.FinishedJointOperationSigners

internal sealed interface JointOperationSnapshot {

    data object NotSynced : JointOperationSnapshot

    data class InFlight(val state: JointOperationState, val isHeld: Boolean = false) : JointOperationSnapshot

    data class Finished(
        val tx: JointOperationTx,
        val signatures: FinishedJointOperationSigners.Signatures,
    ) : JointOperationSnapshot

    data object Unreadable : JointOperationSnapshot
}