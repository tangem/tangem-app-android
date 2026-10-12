package com.tangem.data.jointaccount.blockaid

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.domain.blockaid.models.transaction.ValidationResult
import com.tangem.blockchainsdk.utils.BlockAidChainNameConverter
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.data.jointaccount.safe.SafeTxTypedDataBuilder
import com.tangem.domain.models.network.Network
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import javax.inject.Inject

internal class DefaultJointOperationBlockaidGate @Inject constructor(
    private val blockAidVerifier: BlockAidVerifier,
    private val dispatchers: CoroutineDispatcherProvider,
) : JointOperationBlockaidGate {

    override suspend fun verify(
        network: Network,
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
        ownerAddress: String,
    ): JointOperationBlockaidGate.Verdict = withContext(dispatchers.io) {
        val chain = BlockAidChainNameConverter.convert(network)
            ?: return@withContext JointOperationBlockaidGate.Verdict.UNKNOWN

        // A payload the builder refuses cannot be signed either — the signer reports that; the gate only skips
        val typedDataJson = runCatching {
            SafeTxTypedDataBuilder.buildJson(safeTx = safeTx, chainId = chainId, verifyingContract = safeAddress)
        }.getOrElse { error ->
            TangemLogger.e("Blockaid check of a joint operation skipped: the payload cannot be rendered", error)
            return@withContext JointOperationBlockaidGate.Verdict.UNKNOWN
        }

        val params = buildJsonArray {
            add(JsonPrimitive(value = ownerAddress))
            add(JsonPrimitive(value = typedDataJson))
        }

        blockAidVerifier
            .verifyTransaction(
                data = TransactionData(
                    chain = chain,
                    accountAddress = ownerAddress,
                    method = SIGN_TYPED_DATA_METHOD,
                    domainUrl = DOMAIN_URL,
                    params = TransactionParams.Evm(params = params.toString()),
                ),
            )
            .fold(
                ifLeft = { error ->
                    TangemLogger.e("Blockaid check of a joint operation did not run", error)
                    JointOperationBlockaidGate.Verdict.UNAVAILABLE
                },
                ifRight = ::toVerdict,
            )
    }

    private fun toVerdict(result: CheckTransactionResult): JointOperationBlockaidGate.Verdict {
        return when (result.validation) {
            ValidationResult.SAFE -> JointOperationBlockaidGate.Verdict.SAFE
            ValidationResult.WARNING -> JointOperationBlockaidGate.Verdict.WARNING
            ValidationResult.UNSAFE -> JointOperationBlockaidGate.Verdict.MALICIOUS
            ValidationResult.FAILED_TO_VALIDATE -> JointOperationBlockaidGate.Verdict.UNKNOWN
        }
    }

    private companion object {
        const val SIGN_TYPED_DATA_METHOD = "eth_signTypedData_v4"
        const val DOMAIN_URL = "https://tangem.com"
    }
}