package com.tangem.domain.models.currency.balance

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Refreshes one kind of extra balance — the fetching counterpart of [BalanceContributionProvider].
 *
 * Registered with a Hilt `@IntoSet` binding in the balance type's own module, so balance refreshes pick up a new
 * balance type without the fetching code knowing about it.
 */
interface BalanceContributionFetcher {

    suspend fun fetch(userWalletId: UserWalletId, currencies: Collection<CryptoCurrency>): Either<Throwable, Unit>
}