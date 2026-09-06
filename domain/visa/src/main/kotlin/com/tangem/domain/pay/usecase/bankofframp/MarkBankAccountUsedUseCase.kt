package com.tangem.domain.pay.usecase.bankofframp

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.BankOfframpRecentsStore

class MarkBankAccountUsedUseCase(
    private val recentsStore: BankOfframpRecentsStore,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, bankAccountId: String) {
        recentsStore.markUsed(userWalletId = userWalletId, bankAccountId = bankAccountId)
    }
}