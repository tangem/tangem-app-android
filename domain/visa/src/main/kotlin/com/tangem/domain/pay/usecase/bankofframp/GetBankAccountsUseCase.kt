package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankAccount
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.repository.BankOfframpRepository
import java.util.Locale

/** Saved recipients sorted A–Z by display name, as the "See all" screen shows them. */
class GetBankAccountsUseCase(
    private val repository: BankOfframpRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<BankOfframpError, List<BankAccount>> {
        return repository.getBankAccounts(userWalletId).map { accounts ->
            accounts.sortedBy { it.displayName.lowercase(Locale.ROOT) }
        }
    }
}