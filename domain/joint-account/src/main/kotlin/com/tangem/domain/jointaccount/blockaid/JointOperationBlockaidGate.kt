package com.tangem.domain.jointaccount.blockaid

import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.network.Network

interface JointOperationBlockaidGate {

    suspend fun verify(
        network: Network,
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
        ownerAddress: String,
    ): Verdict

    enum class Verdict {
        SAFE,
        WARNING,
        MALICIOUS,

        /** The check did not run or did not validate — Blockaid guarantees nothing either way */
        UNKNOWN,
    }
}