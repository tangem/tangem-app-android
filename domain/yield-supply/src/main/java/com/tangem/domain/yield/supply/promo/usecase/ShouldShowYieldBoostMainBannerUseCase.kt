package com.tangem.domain.yield.supply.promo.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.YieldBoostPromo
import com.tangem.domain.yield.supply.models.YieldBoostStatus
import com.tangem.domain.yield.supply.promo.YieldPromoRepository

/**
 * Returns `true` iff the main wallet boost banner should be shown:
 *  - promo is `Active` server-side
 *  - status is `NotStarted`
 *
 * Token ownership is intentionally NOT checked — the banner is shown to every eligible wallet
 * regardless of whether it currently holds a promo token.
 *
 * Any repository failure surfaces as `Either.Left` — never assume eligibility on uncertainty.
 * Feature-toggle / redesign / "user dismissed" gating is the caller's responsibility.
 */
class ShouldShowYieldBoostMainBannerUseCase(
    private val repository: YieldPromoRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, Boolean> = Either.catch {
        val promo = repository.getYieldBoostPromo(userWalletId)
        if (promo !is YieldBoostPromo.Active) return@catch false

        val status = repository.getYieldBoostStatus(userWalletId)
        status is YieldBoostStatus.NotStarted
    }
}