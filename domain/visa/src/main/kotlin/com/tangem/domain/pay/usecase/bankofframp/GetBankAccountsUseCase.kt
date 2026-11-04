package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankRail
import com.tangem.domain.pay.repository.BankOfframpRepository

/** Saved non-SPEI recipients in the order the backend returns them. */
class GetBankAccountsUseCase(
    private val repository: BankOfframpRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<BankOfframpError, List<BankAccount>> {
        return repository.getBankAccounts(userWalletId).map(::withoutSpeiRecipients)
    }
}

internal fun withoutSpeiRecipients(accounts: List<BankAccount>): List<BankAccount> =
    accounts.filterNot { it.details.rail == BankRail.SPEI }