package com.tangem.domain.tangempay.usecase.bankofframp

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.bankofframp.BankAccount
import com.tangem.domain.tangempay.model.bankofframp.BankOfframpError
import com.tangem.domain.tangempay.model.bankofframp.BankRail
import com.tangem.domain.tangempay.repository.BankOfframpRepository

/** Saved recipients in the order the backend returns them; CLABE recipients only where SPEI is offered. */
class GetBankAccountsUseCase(
    private val repository: BankOfframpRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        includeSpeiRecipients: Boolean,
    ): Either<BankOfframpError, List<BankAccount>> {
        return repository.getBankAccounts(userWalletId).map { accounts ->
            accounts.withSpeiRecipientsIf(includeSpeiRecipients)
        }
    }
}

internal fun List<BankAccount>.withSpeiRecipientsIf(isIncluded: Boolean): List<BankAccount> =
    if (isIncluded) this else filterNot { it.details.rail == BankRail.SPEI }