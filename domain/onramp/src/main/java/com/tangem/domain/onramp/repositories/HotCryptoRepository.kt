package com.tangem.domain.onramp.repositories

import com.tangem.domain.onramp.model.HotCryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

/**
 * Hot crypto repository
 *
[REDACTED_AUTHOR]
 */
interface HotCryptoRepository {

    /** Fetch hot crypto */
    fun fetchHotCrypto()

    /** Get currencies by [userWalletId] */
    fun getCurrencies(userWalletId: UserWalletId): Flow<List<HotCryptoCurrency>>
}