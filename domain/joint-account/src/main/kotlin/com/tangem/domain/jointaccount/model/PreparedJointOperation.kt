package com.tangem.domain.jointaccount.model

import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network

sealed interface PreparedJointOperation {

    val account: Account.Joint
    val network: Network
    val safeTx: SafeTx
    val verdict: JointOperationBlockaidGate.Verdict

    data class Creation(
        override val account: Account.Joint,
        override val network: Network,
        override val safeTx: SafeTx,
        override val verdict: JointOperationBlockaidGate.Verdict,
        val jointAccount: JointAccount,
        val call: SafeInnerCall,
        val gasToken: SafeGasToken,
        val chainId: Long,
        val safeAddress: String,
        val ownerAddress: String,
        val isRebuilt: Boolean = false,
    ) : PreparedJointOperation

    data class Signing(
        override val account: Account.Joint,
        override val network: Network,
        override val safeTx: SafeTx,
        override val verdict: JointOperationBlockaidGate.Verdict,
        val operationId: String,
        val chainId: Long,
        val safeAddress: String,
    ) : PreparedJointOperation
}