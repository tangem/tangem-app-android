package com.tangem.grow.datasource.express

import com.tangem.domain.models.wallet.UserWallet

/** Provides the Express affiliate ref-code and card batch id for a given wallet. */
interface ExpressUtils {

    suspend fun getRefCode(userWallet: UserWallet?): String?

    /** Batch id of the card behind a cold wallet; null for hot wallets. */
    fun getBatchId(userWallet: UserWallet?): String?
}