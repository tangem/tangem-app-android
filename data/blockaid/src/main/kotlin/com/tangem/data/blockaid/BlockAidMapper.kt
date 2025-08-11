package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.transaction.*
import com.domain.blockaid.models.transaction.simultation.AmountInfo
import com.domain.blockaid.models.transaction.simultation.ApprovedAmount
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.domain.blockaid.models.transaction.simultation.TokenInfo
import com.tangem.blockchain.extensions.hexToBigDecimal
import com.tangem.datasource.api.common.blockaid.models.request.EvmTransactionScanRequest
import com.tangem.datasource.api.common.blockaid.models.request.RpcData
import com.tangem.datasource.api.common.blockaid.models.request.SolanaTransactionScanRequest
import com.tangem.datasource.api.common.blockaid.models.response.*
import org.json.JSONArray

private const val SUCCESS_STATUS = "Success"
private const val DOMAIN_CHECKED_STATUS = "hit"
private const val VALIDATION_SAFE_STATUS = "Benign"
private const val VALIDATION_WARNING_STATUS = "Warning"
private const val VALIDATION_MALICIOUS_STATUS = "Malicious"

private const val SOL_ASSET_SYMBOL = "SOL"

internal object BlockAidMapper {

    fun mapToDomain(from: DomainScanResponse): CheckDAppResult {
        return when {
            from.status != DOMAIN_CHECKED_STATUS -> CheckDAppResult.FAILED_TO_VERIFY
            from.isMalicious == true -> CheckDAppResult.UNSAFE
            else -> CheckDAppResult.SAFE
        }
    }

    fun mapToDomain(from: SolanaTransactionResponse): CheckTransactionResult {
        val validation = when (from.result.validation.resultType) {
            VALIDATION_SAFE_STATUS -> ValidationResult.SAFE
            VALIDATION_WARNING_STATUS -> ValidationResult.WARNING
            VALIDATION_MALICIOUS_STATUS -> ValidationResult.UNSAFE
            else -> ValidationResult.FAILED_TO_VALIDATE
        }
        val simulationResponse = from.result.simulation
        val simulation = if (simulationResponse == null) {
            SimulationResult.FailedToSimulate
        } else {
            mapToSolanaAssetsDiffs(simulationResponse.accountSummary.accountAssetsDiff)
        }
        return CheckTransactionResult(
            validation = validation,
            description = from.result.validation.description,
            simulation = simulation,
        )
    }

    fun mapToDomain(from: TransactionScanResponse): CheckTransactionResult {
        return CheckTransactionResult(
            validation = when {
                from.validation.status != SUCCESS_STATUS -> ValidationResult.FAILED_TO_VALIDATE
                from.validation.resultType == VALIDATION_SAFE_STATUS -> ValidationResult.SAFE
                from.validation.resultType == VALIDATION_WARNING_STATUS -> ValidationResult.WARNING
                else -> ValidationResult.UNSAFE
            },
            simulation = if (from.simulation.status != SUCCESS_STATUS) {
                SimulationResult.FailedToSimulate
            } else {
                mapSimulationSuccessResult(from.simulation.accountSummary)
            },
            description = from.validation.description,
        )
    }

    fun mapToEvmRequest(from: TransactionData): EvmTransactionScanRequest {
        return EvmTransactionScanRequest(
            chain = from.chain.lowercase(),
            accountAddress = from.accountAddress,
            method = from.method,
            data = RpcData(
                method = from.method,
                params = parseParams((from.params as TransactionParams.Evm).params),
            ),
            metadata = TransactionMetadata(from.domainUrl),
        )
    }

    private fun parseParams(rawParams: String): List<Map<String, String>> {
        val jsonArray = JSONArray(rawParams)
        return (0 until jsonArray.length()).map { index ->
            val jsonObject = jsonArray.getJSONObject(index)
            jsonObject.keys().asSequence().associateWith { key -> jsonObject.getString(key) }
        }
    }

    fun mapToSolanaRequest(from: TransactionData): SolanaTransactionScanRequest {
        return SolanaTransactionScanRequest(
            blockchain = from.chain.lowercase(),
            accountAddress = from.accountAddress,
            metadata = TransactionMetadata(from.domainUrl),
            method = from.method,
            transactions = (from.params as TransactionParams.Solana).transactions,
        )
    }

    private fun mapSimulationSuccessResult(from: AccountSummaryResponse): SimulationResult {
        return when {
            !from.assetsDiffs.isNullOrEmpty() -> mapSendReceiveTransaction(
                from.assetsDiffs,
            )
            !from.exposures.isNullOrEmpty() -> mapApproveTransaction(
                from.exposures,
            )
            !from.traces.isNullOrEmpty() -> mapNftSendReceiveTransaction(from.traces)
            else -> SimulationResult.Success(data = SimulationData.NoWalletChangesDetected)
        }
    }

    private fun mapToSolanaAssetsDiffs(assetsDiffs: List<SolanaTransactionAssetDiff>): SimulationResult {
        val sendInfo = assetsDiffs.mapNotNull { assetDiff ->
            val outTransfer = assetDiff.outTransfer ?: return@mapNotNull null
            val amount = outTransfer.amount?.toBigDecimalOrNull() ?: return@mapNotNull null
            AmountInfo.FungibleTokens(
                amount = amount,
                token = TokenInfo(
                    chainId = null,
                    logoUrl = assetDiff.asset.logoUrl,
                    symbol = assetDiff.asset.assetSymbol(),
                    decimals = assetDiff.asset.decimals ?: 0,
                ),
            )
        }
        val receiveInfo = assetsDiffs.mapNotNull { assetDiff ->
            val inTransfer = assetDiff.inTransfer ?: return@mapNotNull null
            val amount = inTransfer.amount?.toBigDecimalOrNull() ?: return@mapNotNull null
            AmountInfo.FungibleTokens(
                amount = amount,
                token = TokenInfo(
                    chainId = null,
                    logoUrl = assetDiff.asset.logoUrl,
                    symbol = assetDiff.asset.assetSymbol(),
                    decimals = assetDiff.asset.decimals ?: 0,
                ),
            )
        }

        return if (sendInfo.isNotEmpty() || receiveInfo.isNotEmpty()) {
            SimulationResult.Success(SimulationData.SendAndReceive(send = sendInfo, receive = receiveInfo))
        } else {
            SimulationResult.Success(SimulationData.NoWalletChangesDetected)
        }
    }

    private fun SolanaTransactionAsset.assetSymbol(): String {
        return if (type?.lowercase().equals(SOL_ASSET_SYMBOL, ignoreCase = true)) {
            symbol ?: SOL_ASSET_SYMBOL
        } else {
            symbol.orEmpty()
        }
    }

    private fun mapApproveTransaction(exposures: List<Exposure>?): SimulationResult {
        val amounts = exposures?.flatMap { exposure ->
            val tokenInfo = TokenInfo(
                chainId = exposure.asset.chainId,
                logoUrl = exposure.asset.logoUrl,
                symbol = exposure.asset.symbol ?: "",
                decimals = exposure.asset.decimals ?: 0,
            )
            exposure.spenders.flatMap { (_, spender) ->
                val isUnlimited = spender.isApprovedForAll == true
                val approval = spender.approval?.hexToBigDecimal()
                spender.exposure.map { detail ->
                    ApprovedAmount(
                        approvedAmount = detail.value?.toBigDecimalOrNull() ?: approval ?: 1.toBigDecimal(),
                        isUnlimited = isUnlimited,
                        tokenInfo = tokenInfo,
                    )
                }
            }
        }
        return if (!amounts.isNullOrEmpty()) {
            SimulationResult.Success(SimulationData.Approve(amounts))
        } else {
            SimulationResult.Success(SimulationData.NoWalletChangesDetected)
        }
    }

    private fun mapSendReceiveTransaction(assetDiffs: List<AssetDiff>?): SimulationResult {
        val sendInfo = arrayListOf<AmountInfo>()
        val receiveInfo = arrayListOf<AmountInfo>()

        assetDiffs?.forEach { diff ->
            val token = TokenInfo(
                chainId = diff.asset.chainId,
                logoUrl = diff.asset.logoUrl,
                symbol = diff.asset.symbol ?: "",
                decimals = diff.asset.decimals ?: 0,
            )
            diff.outTransfer.orEmpty().forEach { transfer ->
                transfer.value?.toBigDecimalOrNull()?.let { amount ->
                    sendInfo.add(AmountInfo.FungibleTokens(amount = amount, token = token))
                }
            }
            diff.inTransfer.orEmpty().forEach { transfer ->
                transfer.value?.toBigDecimalOrNull()?.let { amount ->
                    receiveInfo.add(AmountInfo.FungibleTokens(amount = amount, token = token))
                }
            }
        }

        return if (sendInfo.isNotEmpty() || receiveInfo.isNotEmpty()) {
            SimulationResult.Success(SimulationData.SendAndReceive(send = sendInfo, receive = receiveInfo))
        } else {
            SimulationResult.Success(SimulationData.NoWalletChangesDetected)
        }
    }

    private fun mapNftSendReceiveTransaction(traces: List<Trace>?): SimulationResult {
        val sendInfo = traces?.mapNotNull {
            it.exposed?.let { exposed ->
                AmountInfo.NonFungibleTokens(name = "${it.asset.name} #${exposed.tokenId}", logoUrl = exposed.logoUrl)
            }
        }

        return if (!sendInfo.isNullOrEmpty()) {
            SimulationResult.Success(SimulationData.SendAndReceive(send = sendInfo, receive = listOf()))
        } else {
            SimulationResult.Success(SimulationData.NoWalletChangesDetected)
        }
    }
}