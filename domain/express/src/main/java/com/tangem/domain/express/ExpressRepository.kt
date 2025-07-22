package com.tangem.domain.express

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.models.wallet.UserWallet

/**
 * Express repository
 */
interface ExpressRepository {

    /**
     * Returns list of express providers
     *
     * @param userWallet selected user wallet info
     * @param filterProviderTypes filters only specified provider types, if empty returns providers as is
     */
    suspend fun getProviders(
        userWallet: UserWallet,
        filterProviderTypes: List<ExpressProviderType>,
    ): List<ExpressProvider>
}