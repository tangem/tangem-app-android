package com.tangem.data.blockaid

import com.domain.blockaid.models.dapp.CheckDAppResult
import com.domain.blockaid.models.transaction.*
import com.domain.blockaid.models.transaction.simultation.ApprovedAmount
import com.domain.blockaid.models.transaction.simultation.SimulationData
import com.domain.blockaid.models.transaction.simultation.TokenInfo
import com.domain.blockaid.models.transaction.simultation.AmountInfo
import com.tangem.datasource.api.common.blockaid.models.request.EvmTransactionScanRequest
import com.tangem.datasource.api.common.blockaid.models.request.RpcData
import com.tangem.datasource.api.common.blockaid.models.request.SolanaTransactionScanRequest
import com.tangem.datasource.api.common.blockaid.models.response.*
import javax.inject.Inject

private const val SUCCESS_STATUS = "Success"
private const val DOMAIN_CHECKED_STATUS = "hit"
private const val VALIDATION_SAFE_STATUS = "Benign"

internal class BlockAidMapper @Inject constructor() {

    fun mapToDomain(from: DomainScanResponse): CheckDAppResult {
        return when {
            from.status != DOMAIN_CHECKED_STATUS -> CheckDAppResult.FAILED_TO_VERIFY
            from.isMalicious == true -> CheckDAppResult.UNSAFE
            else -> CheckDAppResult.SAFE
        }
    }

    fun mapToDomain(from: TransactionScanResponse): CheckTransactionResult {
        return CheckTransactionResult(
            validation = when {
                from.validation.status != SUCCESS_STATUS -> ValidationResult.FAILED_TO_VALIDATE
                from.validation.resultType == VALIDATION_SAFE_STATUS -> ValidationResult.SAFE
                else -> ValidationResult.UNSAFE
            },
            simulation = if (from.simulation.status != SUCCESS_STATUS) {
                SimulationResult.FailedToSimulate
            } else {
                mapSimulationSuccessResult(from.simulation.accountSummary)
            },
        )
    }

    fun mapToEvmRequest(from: TransactionData): EvmTransactionScanRequest {
        return EvmTransactionScanRequest(
            chain = from.chain,
            accountAddress = from.accountAddress,
            method = from.method,
            data = RpcData(
                method = from.method,
                params = (from.params as TransactionParams.Evm).params,
            ),
            metadata = TransactionMetadata(from.domainUrl),
        )
    }

    fun mapToSolanaRequest(from: TransactionData): SolanaTransactionScanRequest {
        return SolanaTransactionScanRequest(
            chain = from.chain,
            accountAddress = from.accountAddress,
            metadata = TransactionMetadata(from.domainUrl),
            method = from.method,
            transactions = (from.params as TransactionParams.Solana).transactions,
        )
    }

    private fun mapSimulationSuccessResult(from: AccountSummaryResponse): SimulationResult {
        return when {
            from.assetsDiffs.isEmpty() && from.exposures.isNotEmpty() -> mapApproveTransaction(from.exposures)
            from.assetsDiffs.isNotEmpty() && from.exposures.isEmpty() -> mapSendReceiveTransaction(from.assetsDiffs)
            else -> SimulationResult.FailedToSimulate
        }
    }

    private fun mapApproveTransaction(exposures: List<Exposure>): SimulationResult {
        val amounts = exposures.flatMap { exposure ->
            val tokenInfo = TokenInfo(
                chainId = exposure.asset.chainId,
                logoUrl = exposure.asset.logoUrl,
                symbol = exposure.asset.symbol,
            )
            exposure.spenders.flatMap { (_, spender) ->
                val isUnlimited = spender.isApprovedForAll == true
                spender.exposure.mapNotNull { detail ->
                    ApprovedAmount(
                        approvedAmount = detail.value.toBigDecimalOrNull() ?: return@mapNotNull null,
                        isUnlimited = isUnlimited,
                        tokenInfo = tokenInfo,
                    )
                }
            }
        }
        return if (amounts.isNotEmpty()) {
            SimulationResult.Success(SimulationData.Approve(amounts))
        } else {
            SimulationResult.FailedToSimulate
        }
    }

    private fun mapSendReceiveTransaction(assetDiffs: List<AssetDiff>): SimulationResult {
        val sendInfo = arrayListOf<AmountInfo>()
        val receiveInfo = arrayListOf<AmountInfo>()

        assetDiffs.forEach { diff ->
            val token = TokenInfo(
                chainId = diff.asset.chainId,
                logoUrl = diff.asset.logoUrl,
                symbol = diff.asset.symbol,
            )
            diff.outTransfer.orEmpty().forEach { transfer ->
                transfer.value.toBigDecimalOrNull()?.let { amount ->
                    sendInfo.add(AmountInfo(amount = amount, token = token))
                }
            }
            diff.inTransfer.orEmpty().forEach { transfer ->
                transfer.value.toBigDecimalOrNull()?.let { amount ->
                    receiveInfo.add(AmountInfo(amount = amount, token = token))
                }
            }
        }

        return if (sendInfo.isNotEmpty() || receiveInfo.isNotEmpty()) {
            SimulationResult.Success(SimulationData.SendAndReceive(send = sendInfo, receive = receiveInfo))
        } else {
            SimulationResult.FailedToSimulate
        }
    }
}