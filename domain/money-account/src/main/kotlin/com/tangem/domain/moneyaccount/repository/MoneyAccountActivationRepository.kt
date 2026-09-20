package com.tangem.domain.moneyaccount.repository

import com.tangem.domain.models.wallet.UserWalletId

interface MoneyAccountActivationRepository {

    /**
     * Derives the Money Account key on the card (NFC) and persists it into the wallet, so the
     * on-chain money account balance can later be fetched without re-deriving. Throws on failure.
     */
    @Throws
    suspend fun activateMoneyAccount(userWalletId: UserWalletId)
}