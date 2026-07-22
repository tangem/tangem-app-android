package com.tangem.domain.pay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.CashbackRepository

/**
 * Reads whether the "Cashback deactivated" banner was permanently dismissed for the wallet.
 *
 * Used by the Payment account cashback block to decide whether to show the deactivation banner.
 */
class GetCashbackDeactivationDismissedUseCase(
    private val cashbackRepository: CashbackRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Boolean {
        return cashbackRepository.isDeactivationBannerDismissed(userWalletId)
    }
}