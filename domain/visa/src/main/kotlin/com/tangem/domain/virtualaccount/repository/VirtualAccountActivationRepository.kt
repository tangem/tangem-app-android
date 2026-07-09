package com.tangem.domain.virtualaccount.repository

import com.tangem.domain.models.wallet.UserWalletId

interface VirtualAccountActivationRepository {

    /**
     * Derives the Virtual Account key on the card (NFC) and persists it into the wallet, so the
     * on-chain VA balance can later be fetched without re-deriving. Throws on failure.
     */
    @Throws
    suspend fun activateVirtualAccount(userWalletId: UserWalletId)
}