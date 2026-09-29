package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.RecentBankAccounts
import com.tangem.domain.pay.repository.BankOfframpRecentsStore
import com.tangem.domain.pay.repository.BankOfframpRepository
import java.util.Locale

/**
 * Up to three saved recipients: the ones the app's own usage log knows come first, newest first, and the rest
 * follow in the "See all" order, so recipients added earlier or on another device still show up. Both the
 * shortlist and whether anyone is left out come out of one request, so the hub never asks twice.
 */
class GetRecentBankAccountsUseCase(
    private val repository: BankOfframpRepository,
    private val recentsStore: BankOfframpRecentsStore,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<BankOfframpError, RecentBankAccounts> = either {
        val accounts = repository.getBankAccounts(userWalletId).bind()
        val usage = recentsStore.getUsage(userWalletId)

        RecentBankAccounts(
            recents = accounts
                .sortedWith(
                    compareByDescending<BankAccount> { usage[it.id] }
                        .thenBy { it.displayName.lowercase(Locale.ROOT) },
                )
                .take(MAX_RECENTS),
            hasMoreAccounts = accounts.size > MAX_RECENTS,
        )
    }

    private companion object {
        const val MAX_RECENTS = 3
    }
}