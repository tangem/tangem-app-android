package com.tangem.domain.express

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.wallets.models.UserWallet

/**
 * Express repository
 */
interface ExpressRepository {

    /**
     * Returns list of express providers
     *
     * @param userWallet selected user wallet info
     */
    suspend fun getProviders(userWallet: UserWallet): List<ExpressProvider>
}