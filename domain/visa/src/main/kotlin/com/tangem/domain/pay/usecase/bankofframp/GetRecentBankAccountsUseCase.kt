package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.RecentBankAccounts
import com.tangem.domain.pay.repository.BankOfframpRepository

/**
 * Up to three saved recipients in the order the backend returns them. Both the shortlist and whether anyone is left
 * out come out of one request, so the hub never asks twice.
 */
class GetRecentBankAccountsUseCase(
    private val repository: BankOfframpRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<BankOfframpError, RecentBankAccounts> {
        return repository.getBankAccounts(userWalletId).map { accounts ->
            RecentBankAccounts(
                recents = accounts.take(MAX_RECENTS),
                hasMoreAccounts = accounts.size > MAX_RECENTS,
            )
        }
    }

    private companion object {
        const val MAX_RECENTS = 3
    }
}