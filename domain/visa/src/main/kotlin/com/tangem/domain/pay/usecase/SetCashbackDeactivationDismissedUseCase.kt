package com.tangem.domain.pay.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.CashbackRepository

/**
 * Permanently dismisses the "Cashback deactivated" banner for the wallet.
 *
 * Triggered by the "Got it" button on the Payment account cashback deactivation banner.
 */
class SetCashbackDeactivationDismissedUseCase(
    private val cashbackRepository: CashbackRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId) {
        cashbackRepository.setDeactivationBannerDismissed(userWalletId)
    }
}