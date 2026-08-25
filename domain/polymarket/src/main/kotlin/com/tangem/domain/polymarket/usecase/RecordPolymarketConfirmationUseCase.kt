package com.tangem.domain.polymarket.usecase

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.model.PolymarketWalletStatus

/**
 * Keeps the local record of [PolymarketOnboardedStore] in step with what the backend reports about a wallet.
 *
 * The entry gate and the wallet-screen refresh both read that status and both have to answer the same question,
 * so the rule lives here instead of in each of them.
 *

 * stating there is no wallet — the shape a backend-side reset takes. Every other status describes a setup in
 * flight, a failed one, or no answer at all, none of which unmakes a readiness already confirmed: a refresh
 * landing on one must not erase what a finished onboarding has just written.
 */
class RecordPolymarketConfirmationUseCase(
    private val onboardedStore: PolymarketOnboardedStore,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, status: PolymarketWalletStatus) {
        when (status) {
            PolymarketWalletStatus.READY_TO_TRADE -> onboardedStore.markOnboarded(userWalletId)
            PolymarketWalletStatus.NOT_CREATED -> onboardedStore.clear(userWalletId)
            PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS,
            PolymarketWalletStatus.DEPLOYMENT_FAILED,
            PolymarketWalletStatus.DEPLOYED,
            PolymarketWalletStatus.APPROVALS_IN_PROGRESS,
            PolymarketWalletStatus.APPROVALS_FAILED,
            PolymarketWalletStatus.UNKNOWN,
            -> Unit
        }
    }
}