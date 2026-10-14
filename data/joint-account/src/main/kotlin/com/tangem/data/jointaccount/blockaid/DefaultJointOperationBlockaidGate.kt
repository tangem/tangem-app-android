package com.tangem.data.jointaccount.blockaid

import com.domain.blockaid.models.transaction.CheckTransactionResult
import com.domain.blockaid.models.transaction.TransactionData
import com.domain.blockaid.models.transaction.TransactionParams
import com.domain.blockaid.models.transaction.ValidationResult
import com.tangem.blockchainsdk.utils.BlockAidChainNameConverter
import com.tangem.data.jointaccount.safe.SafeTxTypedDataBuilder
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.jointaccount.blockaid.JointOperationBlockaidGate
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.safe.SafeDeployment
import com.tangem.domain.models.network.Network
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addHexPrefix
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class DefaultJointOperationBlockaidGate @Inject constructor(
    private val blockAidVerifier: BlockAidVerifier,
    private val dispatchers: CoroutineDispatcherProvider,
) : JointOperationBlockaidGate {

    @Suppress("LongParameterList")
    override suspend fun verify(
        network: Network,
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
        ownerAddress: String,
        deployment: SafeDeployment,
    ): JointOperationBlockaidGate.Verdict = withContext(dispatchers.io) {
        val chain = BlockAidChainNameConverter.convert(network)
            ?: return@withContext JointOperationBlockaidGate.Verdict.UNKNOWN

        val request = when (deployment) {
            SafeDeployment.DEPLOYED -> typedDataRequest(
                chain = chain,
                safeTx = safeTx,
                chainId = chainId,
                safeAddress = safeAddress,
                ownerAddress = ownerAddress,
            ) ?: return@withContext JointOperationBlockaidGate.Verdict.UNKNOWN
            SafeDeployment.NOT_DEPLOYED -> innerCallRequest(chain = chain, safeTx = safeTx, safeAddress = safeAddress)
        }

        blockAidVerifier
            .verifyTransaction(data = request)
            .fold(
                ifLeft = { error ->
                    TangemLogger.e("Blockaid check of a joint operation did not run", error)
                    JointOperationBlockaidGate.Verdict.UNAVAILABLE
                },
                ifRight = ::toVerdict,
            )
    }

    private fun typedDataRequest(
        chain: String,
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
        ownerAddress: String,
    ): TransactionData? {
        // A payload the builder refuses cannot be signed either — the signer reports that; the gate only skips
        val typedDataJson = runCatching {
            SafeTxTypedDataBuilder.buildJson(safeTx = safeTx, chainId = chainId, verifyingContract = safeAddress)
        }.getOrElse { error ->
            TangemLogger.e("Blockaid check of a joint operation skipped: the payload cannot be rendered", error)
            return null
        }

        val params = buildJsonArray {
            add(JsonPrimitive(value = ownerAddress))
            add(JsonPrimitive(value = typedDataJson))
        }

        return TransactionData(
            chain = chain,
            accountAddress = ownerAddress,
            method = SIGN_TYPED_DATA_METHOD,
            domainUrl = DOMAIN_URL,
            params = TransactionParams.Evm(params = params.toString()),
        )
    }

    private fun innerCallRequest(chain: String, safeTx: SafeTx, safeAddress: String): TransactionData {
        val params = buildJsonArray {
            add(
                buildJsonObject {
                    put(key = "from", value = safeAddress)
                    put(key = "to", value = safeTx.to.addHexPrefix())
                    put(key = "value", value = HEX_PREFIX + safeTx.value.toString(HEX_RADIX))
                    put(key = "data", value = safeTx.data.addHexPrefix())
                },
            )
        }

        return TransactionData(
            chain = chain,
            accountAddress = safeAddress,
            method = SIGN_TRANSACTION_METHOD,
            domainUrl = null,
            params = TransactionParams.Evm(params = params.toString()),
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
        const val SIGN_TRANSACTION_METHOD = "eth_signTransaction"
        const val DOMAIN_URL = "https://tangem.com"
        const val HEX_PREFIX = "0x"
        const val HEX_RADIX = 16
    }
}