package com.tangem.feature.swap.converters

import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.collectibles.vault.GachaVaultError
import com.tangem.domain.transaction.error.SolanaGaslessError
import com.tangem.feature.swap.domain.models.ui.SwapTransactionState
import com.tangem.utils.converter.Converter

/** The swap error for a failed withdrawal swap out of the collectibles vault; null when the user cancelled it. */
internal object CollectiblesSwapErrorConverter : Converter<CollectiblesAccountError, SwapTransactionState.Error?> {

    override fun convert(value: CollectiblesAccountError): SwapTransactionState.Error? = when (value) {
        CollectiblesAccountError.Cancelled -> null
        is CollectiblesAccountError.MaybeSent -> gasless(SolanaGaslessError.MaybeSent(value.txHash))
        is CollectiblesAccountError.Vault -> when (value.error) {
            GachaVaultError.SponsorUnavailable -> gasless(
                SolanaGaslessError.ServiceUnavailable(code = null, isSponsorshipUnavailable = true),
            )
            GachaVaultError.SponsorRateLimited -> gasless(SolanaGaslessError.RateLimited(isPerWallet = false))
            GachaVaultError.TransactionExpired -> gasless(SolanaGaslessError.BlockhashExpired)
            else -> SwapTransactionState.Error.UnknownError
        }
        else -> SwapTransactionState.Error.UnknownError
    }

    private fun gasless(error: SolanaGaslessError) = SwapTransactionState.Error.SolanaGaslessError(error)
}