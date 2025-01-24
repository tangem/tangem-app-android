package com.tangem.domain.onramp.repositories

import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Hot crypto repository
 *
[REDACTED_AUTHOR]
 */
interface HotCryptoRepository {

    /** Get currencies by [userWalletId] */
    fun getCurrencies(userWalletId: UserWalletId): Flow<List<HotCryptoCurrency>>
}