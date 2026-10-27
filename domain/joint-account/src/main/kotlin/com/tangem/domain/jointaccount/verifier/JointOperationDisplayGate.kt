package com.tangem.domain.jointaccount.verifier

import com.tangem.domain.jointaccount.gas.JointGasTokenResolver
import com.tangem.domain.jointaccount.gas.JointRefundReceiverProvider
import com.tangem.domain.jointaccount.gas.gasTokenQuoteId
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.model.JointOperation
import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.network.Network
import com.tangem.utils.logging.TangemLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * An operation the Sign tap would refuse is not shown at all; its slot staying busy is accepted. The gas price
 * ceiling needs the network and stays with the Sign tap.
 */
class JointOperationDisplayGate(
    private val verifier: JointOperationVerifier,
    private val compositionVerifier: JointAccountCompositionVerifier,
    private val refundReceiverProvider: JointRefundReceiverProvider,
    private val gasTokenResolver: JointGasTokenResolver,
) {

    private val verdicts = ConcurrentHashMap<Verdict, Boolean>()

    suspend fun isShown(
        account: Account.Joint,
        jointAccount: JointAccount,
        operation: JointOperation,
        network: Network,
        transfer: JointOperationTransfer,
    ): Boolean {
        val ownerAddress = compositionVerifier.ownerAddress(
            userWalletId = account.accountId.userWalletId,
            ownerKeyIndex = account.ownerKeyIndex,
        )
        if (ownerAddress == null) {
            TangemLogger.e("Joint operation ${operation.operationId} is hidden: the owner address is not derived")
            return false
        }

        val verdict = Verdict(
            operation = operation,
            composition = Composition(jointAccount),
            ownerAddress = ownerAddress,
            networkId = network.id,
        )
        verdicts[verdict]?.let { return it }

        // Not cached: the gas token is priced from quotes that may simply not have arrived yet
        val refundBounds = refundBounds(account = account, network = network, operation = operation)
        if (refundBounds == null) {
            TangemLogger.e("Joint operation ${operation.operationId} is hidden: the refund bounds cannot be read")
            return false
        }

        val result = verifier.verify(
            jointAccount = jointAccount,
            operation = operation,
            displayed = transfer.intent,
            ownerAddress = ownerAddress,
            refundBounds = refundBounds,
        )
        result.onLeft { error ->
            TangemLogger.e("Joint operation ${operation.operationId} is hidden: it does not verify, $error")
        }

        if (verdicts.size >= MAX_VERDICTS) verdicts.clear()
        return result.isRight().also { verdicts[verdict] = it }
    }

    private suspend fun refundBounds(
        account: Account.Joint,
        network: Network,
        operation: JointOperation,
    ): JointOperationVerifier.RefundBounds? {
        val refundReceiver = runCatching { refundReceiverProvider.refundReceiver() }.getOrNull() ?: return null
        val gasToken = gasTokenResolver
            .resolve(
                network = network,
                address = operation.safeTx.gasToken,
                rawCurrencyId = account.gasTokenQuoteId(network = network, gasToken = operation.safeTx.gasToken),
            )
            .getOrNull()
            ?: return null

        return JointOperationVerifier.RefundBounds(
            refundReceiver = refundReceiver,
            networkGasPrice = null,
            gasToken = gasToken,
        )
    }

    private data class Verdict(
        val operation: JointOperation,
        val composition: Composition,
        val ownerAddress: String,
        val networkId: Network.ID,
    )

    private data class Composition(
        val cryptoAccountId: String,
        val members: List<JointAccount.Member>,
        val threshold: Int,
        val safeAddress: String?,
    ) {
        constructor(jointAccount: JointAccount) : this(
            cryptoAccountId = jointAccount.cryptoAccountId,
            members = jointAccount.members,
            threshold = jointAccount.threshold,
            safeAddress = jointAccount.safeAddress,
        )
    }

    private companion object {
        const val MAX_VERDICTS = 64
    }
}