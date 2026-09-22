package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.RecentBankAccounts
import com.tangem.domain.pay.repository.BankOfframpRecentsStore
import com.tangem.domain.pay.repository.BankOfframpRepository

/**
 * Up to three most recently used recipients, newest first, by the app's own usage log, together with whether the
 * account has any saved recipient at all. Both come out of one request, so the hub never asks twice.
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
                .filter { it.id in usage }
                .sortedByDescending { usage.getValue(it.id) }
                .take(MAX_RECENTS),
            hasSavedAccounts = accounts.isNotEmpty(),
        )
    }

    private companion object {
        const val MAX_RECENTS = 3
    }
}