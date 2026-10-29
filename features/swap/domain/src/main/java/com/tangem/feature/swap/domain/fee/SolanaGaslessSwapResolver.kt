package com.tangem.feature.swap.domain.fee

import arrow.core.getOrElse

import com.tangem.blockchain.blockchains.solana.SolanaTransactionHelper
import com.tangem.blockchain.blockchains.solana.gasless.SolanaGaslessSimulation
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.extensions.Result
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.transaction.SolanaGaslessTransactionRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.feature.swap.domain.models.domain.ExpressTransactionModel
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.lib.crypto.BlockchainUtils.SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES
import com.tangem.lib.crypto.BlockchainUtils.isSolana
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Base64
import javax.inject.Inject

/**
 * Decides whether the fee of a Solana DEX swap can be sponsored, and by whom.
 * Calling the fee-payer route doubles as the availability check: it fails while the service has Solana switched off
 * or rejects the api key. The sponsored tx is then simulated, because the sponsor pays only the fee: rent for
 * a new token account still comes from the user, and the backend rejects such a tx only after the user has signed.
 */
internal class SolanaGaslessSwapResolver @Inject constructor(
    private val swapFeatureToggles: SwapFeatureToggles,
    private val solanaGaslessTransactionRepository: SolanaGaslessTransactionRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {

    private val resolutionMutex = Mutex()
    private var lastResolution: Resolution? = null

    /**
     * Hides the providers whose txs a sponsor cannot carry, so a swap that may be sponsored falls back to another
     * provider instead of failing on it.
     */
    fun excludeUnsupportedProviders(fromCurrency: CryptoCurrency, providers: List<SwapProvider>): List<SwapProvider> {
        if (!isSponsorable(fromCurrency)) return providers
        return providers.filterNot { it.providerId in UNSUPPORTED_PROVIDER_IDS }
    }

    suspend fun resolveFeePayer(fromSwapCurrencyStatus: SwapCurrencyStatus, swapData: SwapDataModel?): String? {
        if (!isSponsorable(fromSwapCurrencyStatus.currency)) return null
        val txData = (swapData?.transaction as? ExpressTransactionModel.DEX)?.txData ?: return null

        // A single fee load asks several times for the same quote; each ask would re-parse and log it, and the
        // simulation is a node round trip
        return resolutionMutex.withLock {
            val key = Resolution.Key(userWalletId = fromSwapCurrencyStatus.userWalletId, txData = txData)
            lastResolution?.takeIf { it.key == key }?.let { return@withLock it.feePayer }

            if (!fitsFeePayer(txData, fromSwapCurrencyStatus.userWallet)) {
                lastResolution = Resolution(key = key, feePayer = null)
                return@withLock null
            }
            val feePayer = solanaGaslessTransactionRepository.getFeePayer().getOrElse { error ->
                TangemLogger.i("Solana gasless swap is not offered, fee payer unavailable: $error")
                return@withLock null
            }
            feePayer.takeIf { simulates(fromSwapCurrencyStatus, txData, feePayer) }
                .also { lastResolution = Resolution(key = key, feePayer = it) }
        }
    }

    /** Fails open: an unreachable node must not hide gasless, the backend simulates the tx again on submit. */
    // Swapping from SOL itself means wrapping it into a rent-paying wSOL account the user cannot fund
    private fun isSponsorable(fromCurrency: CryptoCurrency): Boolean = swapFeatureToggles.isSolanaGaslessEnabled &&
        fromCurrency is CryptoCurrency.Token &&
        isSolana(fromCurrency.network.rawId)

    private suspend fun simulates(
        fromSwapCurrencyStatus: SwapCurrencyStatus,
        txData: String,
        feePayer: String,
    ): Boolean {
        val simulation = walletManagersFacade.simulateSolanaGaslessTransaction(
            userWalletId = fromSwapCurrencyStatus.userWalletId,
            network = fromSwapCurrencyStatus.currency.network,
            transactionData = TransactionData.Compiled(
                value = TransactionData.Compiled.Data.Bytes(Base64.getDecoder().decode(txData)),
            ),
            feePayer = feePayer,
        )
        val failed = (simulation as? Result.Success)?.data as? SolanaGaslessSimulation.Failed ?: return true
        TangemLogger.i("Solana gasless swap is not offered, simulation failed: ${failed.error}")
        return false
    }

    private fun fitsFeePayer(txDataBase64: String, userWallet: UserWallet): Boolean = runCatching {
        val transaction = Base64.getDecoder().decode(txDataBase64)
        // The backend fills only the fee payer's slot, so a provider tx needing another co-signer cannot be sponsored
        val signerCount = SolanaTransactionHelper.requiredSignerCount(transaction)
        if (signerCount != 1) {
            TangemLogger.i("Solana gasless swap is not offered, provider tx needs $signerCount signers")
            return@runCatching false
        }

        val messageSize = SolanaTransactionHelper.extractMessage(transaction).size
        val isWithinBackendLimit = messageSize <= SolanaTransactionHelper.MAX_GASLESS_MESSAGE_SIZE
        // The card signs the message after the rewrite, which adds the fee payer key
        val isWithinCardLimit = userWallet !is UserWallet.Cold ||
            messageSize + SOLANA_PUBLIC_KEY_LENGTH <= SOLANA_TRANSACTION_SIZE_THRESHOLD_BYTES
        if (!isWithinBackendLimit || !isWithinCardLimit) {
            TangemLogger.i(
                "Solana gasless swap is not offered, message is $messageSize B: " +
                    "backend limit ok = $isWithinBackendLimit, card limit ok = $isWithinCardLimit",
            )
        }
        isWithinBackendLimit && isWithinCardLimit
    }.getOrElse { error ->
        TangemLogger.e("Solana gasless swap is not offered, provider tx cannot be parsed", error)
        false
    }

    private data class Resolution(val key: Key, val feePayer: String?) {
        data class Key(val userWalletId: UserWalletId, val txData: String)
    }

    private companion object {
        const val SOLANA_PUBLIC_KEY_LENGTH = 32
        const val LI_FI_PROVIDER_ID = "li-fi"

        val UNSUPPORTED_PROVIDER_IDS = setOf(LI_FI_PROVIDER_ID)
    }
}