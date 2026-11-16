package com.tangem.domain.common.wallets

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Removes data a feature keeps for specific currencies when they are removed from a wallet.
 *
 * Registered with a Hilt `@IntoSet` binding in the owning feature's module, so a feature adds its cleanup there
 * without the currency-management code having to know about it.
 */
interface CryptoCurrencyDataCleaner {

    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>)

    /** Removes everything kept for [userWalletIds]; called on wallet deletion, when the currencies are unknown. */
    suspend fun clear(userWalletIds: List<UserWalletId>)
}