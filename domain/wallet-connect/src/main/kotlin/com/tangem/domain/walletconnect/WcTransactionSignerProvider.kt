package com.tangem.domain.walletconnect

import com.tangem.blockchain.common.TransactionSigner
import com.tangem.domain.models.wallet.UserWallet

/**
 * Provider for creating transaction signers for WalletConnect operations.
 *
 * This interface abstracts the creation of [TransactionSigner] instances
 * to avoid direct dependency on card SDK configuration in wallet-connect module.
 */
interface WcTransactionSignerProvider {

    /**
     * Creates a transaction signer for the given wallet.
     *
     * @param wallet The user wallet to create a signer for
     * @return A [TransactionSigner] instance for the wallet
     */
    fun createSigner(wallet: UserWallet): TransactionSigner
}