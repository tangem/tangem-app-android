package com.tangem.data.collectibles.vault

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import io.mockk.every
import io.mockk.mockk

private const val TEST_CARD_BATCH_ID = "99FF"
private const val TEST_CARD_ID = "FF99000000000000"

/** A cold wallet whose card is a test one exactly when [isTestCard] — the batch and the id are what marks it. */
internal fun mockColdWallet(
    walletId: UserWalletId = UserWalletId("011"),
    isTestCard: Boolean = false,
): UserWallet.Cold = mockk {
    every { this@mockk.walletId } returns walletId
    every { scanResponse } returns mockk {
        every { card } returns mockk {
            every { batchId } returns if (isTestCard) TEST_CARD_BATCH_ID else "AB01"
            every { cardId } returns if (isTestCard) TEST_CARD_ID else "AB01000000000000"
        }
        every { productType } returns ProductType.Wallet2
        every { walletData } returns null
    }
}

/** The card kind and the chain the vault is then addressed on. */
internal data class VaultChainModel(val isTestCard: Boolean, val blockchain: Blockchain)

internal fun vaultChainModels() = listOf(
    VaultChainModel(isTestCard = false, blockchain = Blockchain.Solana),
    VaultChainModel(isTestCard = true, blockchain = Blockchain.SolanaTestnet),
)