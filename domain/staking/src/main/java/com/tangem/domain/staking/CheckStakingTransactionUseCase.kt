package com.tangem.domain.staking

import com.domain.blockaid.models.transaction.ValidationResult
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.blockaid.BlockAidVerifier
import com.tangem.domain.models.staking.NetworkType
import com.tangem.domain.staking.analytics.StakingAnalyticsEvent
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.staking.verification.StakingBlockAidRequestFactory
import com.tangem.domain.staking.verification.StakingTransactionRecognizer
import kotlinx.coroutines.CancellationException

/**
 * Verifies a StakeKit staking transaction before signing ("move away from blind signing").
 *
 * Local recognition runs first as a gate; for EVM/Solana, recognized txs are then scanned by Blockaid.
 * Returns a [StakingTransactionVerdict]:
 * - SAFE  → allow, no warning.
 * - WARNING → allow, but surface a suspicious-transaction warning.
 * - UNSAFE → must block (Blockaid UNSAFE, unrecognized local tx, or missing payload).
 *
 * Blockaid no-answer (FAILED_TO_VALIDATE, Left, exception) → SAFE, because local recognition already
 * confirmed the transaction is staking. Disabled (returns SAFE, no checks) when the toggle is off.
 *
 * Emits a [StakingAnalyticsEvent.ScamVerification] for every actually-checked transaction
 * (EVM/Solana/Tron/Cosmos/Cardano); not for toggle-off, pass-through networks, or null payload.
 */
class CheckStakingTransactionUseCase(
    private val blockAidVerifier: BlockAidVerifier,
    private val recognizer: StakingTransactionRecognizer,
    private val requestFactory: StakingBlockAidRequestFactory,
    private val stakingFeatureToggles: StakingFeatureToggles,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    @Suppress("LongParameterList")
    suspend operator fun invoke(
        network: NetworkType,
        accountAddress: String,
        unsignedTransaction: String?,
        token: String,
        blockchain: String,
        provider: String,
    ): StakingTransactionVerdict {
        if (!stakingFeatureToggles.isTransactionValidationEnabled) return StakingTransactionVerdict.SAFE
        val unsigned = unsignedTransaction ?: return StakingTransactionVerdict.UNSAFE

        return when (network) {
            // Ethereum has no local staking signature (no entry in the Notion doc, like iOS): it is
            // scanned by Blockaid only, without the local recognition gate.
            NetworkType.ETHEREUM -> verifyWithBlockAid(
                network = network,
                accountAddress = accountAddress,
                unsignedTransaction = unsigned,
                token = token,
                blockchain = blockchain,
                provider = provider,
            )
            NetworkType.POLYGON,
            NetworkType.BINANCE,
            NetworkType.SOLANA,
            -> if (recognizer.isRecognizedStakingTransaction(network, unsigned)) {
                verifyWithBlockAid(
                    network = network,
                    accountAddress = accountAddress,
                    unsignedTransaction = unsigned,
                    token = token,
                    blockchain = blockchain,
                    provider = provider,
                )
            } else {
                sendScamVerification(
                    token = token,
                    blockchain = blockchain,
                    provider = provider,
                    blockaid = BLOCKAID_NOT_PERFORMED,
                    isRecognized = false,
                )
                StakingTransactionVerdict.UNSAFE
            }
            NetworkType.TRON,
            NetworkType.COSMOS,
            NetworkType.CARDANO,
            -> {
                val isRecognized = recognizer.isRecognizedStakingTransaction(network, unsigned)
                sendScamVerification(
                    token = token,
                    blockchain = blockchain,
                    provider = provider,
                    blockaid = BLOCKAID_NOT_PERFORMED,
                    isRecognized = isRecognized,
                )
                if (isRecognized) StakingTransactionVerdict.SAFE else StakingTransactionVerdict.UNSAFE
            }
            else -> StakingTransactionVerdict.SAFE
        }
    }

    @Suppress("LongParameterList")
    private suspend fun verifyWithBlockAid(
        network: NetworkType,
        accountAddress: String,
        unsignedTransaction: String,
        token: String,
        blockchain: String,
        provider: String,
    ): StakingTransactionVerdict {
        val labelToVerdict = try {
            val data = requestFactory.create(network, accountAddress, unsignedTransaction)
            blockAidVerifier.verifyTransaction(data).fold(
                ifLeft = { BLOCKAID_FAILED to StakingTransactionVerdict.SAFE },
                ifRight = { result ->
                    when (result.validation) {
                        ValidationResult.SAFE -> BLOCKAID_SAFE to StakingTransactionVerdict.SAFE
                        ValidationResult.WARNING -> BLOCKAID_WARNING to StakingTransactionVerdict.WARNING
                        ValidationResult.UNSAFE -> BLOCKAID_UNSAFE to StakingTransactionVerdict.UNSAFE
                        ValidationResult.FAILED_TO_VALIDATE -> BLOCKAID_FAILED to StakingTransactionVerdict.SAFE
                    }
                },
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BLOCKAID_FAILED to StakingTransactionVerdict.SAFE
        }

        sendScamVerification(
            token = token,
            blockchain = blockchain,
            provider = provider,
            blockaid = labelToVerdict.first,
            isRecognized = true,
        )
        return labelToVerdict.second
    }

    private fun sendScamVerification(
        token: String,
        blockchain: String,
        provider: String,
        blockaid: String,
        isRecognized: Boolean,
    ) {
        analyticsEventHandler.send(
            StakingAnalyticsEvent.ScamVerification(
                token = token,
                blockchain = blockchain,
                provider = provider,
                blockaid = blockaid,
                mobileCheck = isRecognized.toString(),
            ),
        )
    }

    private companion object {
        const val BLOCKAID_SAFE = "Safe"
        const val BLOCKAID_WARNING = "Warning"
        const val BLOCKAID_UNSAFE = "Unsafe"
        const val BLOCKAID_FAILED = "Failed to validate"
        const val BLOCKAID_NOT_PERFORMED = "Not performed"
    }
}