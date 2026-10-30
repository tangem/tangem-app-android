package com.tangem.domain.jointaccount.blockaid

import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.SafeDeployment
import com.tangem.domain.models.network.Network

interface JointOperationBlockaidGate {

    /**
     * A deployed Safe is checked by the typed data its owner signs. Blockaid reads the Safe contract to judge that, so
     * before the first operation deploys it the inner call is checked instead, as a transaction the Safe sends.
     */
    @Suppress("LongParameterList")
    suspend fun verify(
        network: Network,
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
        ownerAddress: String,
        deployment: SafeDeployment,
    ): Verdict

    enum class Verdict {
        SAFE,
        WARNING,
        MALICIOUS,

        /** Blockaid answered, but could not validate the payload — or the check could not be built for it at all */
        UNKNOWN,

        /** Blockaid did not answer: the request failed on the way, or the answer could not be read */
        UNAVAILABLE,
    }
}