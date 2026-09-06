package com.tangem.domain.pay.repository

import com.tangem.domain.models.wallet.UserWalletId

/** Local usage log of bank off-ramp recipients; the BFF does not track recents, the app does. */
interface BankOfframpRecentsStore {

    suspend fun markUsed(userWalletId: UserWalletId, bankAccountId: String)

    /** `bankAccountId` → epoch millis of the last use. */
    suspend fun getUsage(userWalletId: UserWalletId): Map<String, Long>
}