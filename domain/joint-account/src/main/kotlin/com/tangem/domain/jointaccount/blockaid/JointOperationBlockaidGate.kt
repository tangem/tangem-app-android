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

    sealed interface Verdict {

        data object Safe : Verdict

        data object Warning : Verdict

        data object Malicious : Verdict

        data class NoVerdict(val reason: Reason) : Verdict {

            enum class Reason {
                ERROR,
                TIMEOUT,
                UNSUPPORTED_NETWORK,
            }
        }
    }
}