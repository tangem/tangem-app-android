package com.tangem.grow.datasource.express

import com.tangem.domain.models.wallet.UserWallet

/** Provides the Express affiliate ref-code for a given wallet. */
interface ExpressUtils {

    suspend fun getRefCode(userWallet: UserWallet?): String?
}