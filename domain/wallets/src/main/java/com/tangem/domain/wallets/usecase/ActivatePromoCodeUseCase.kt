package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.PromoCodeActivation
import com.tangem.domain.wallets.models.PromoPayout
import com.tangem.domain.wallets.models.errors.ActivatePromoCodeError
import com.tangem.domain.wallets.repository.WalletsRepository
import javax.inject.Inject

class ActivatePromoCodeUseCase @Inject constructor(
    private val walletsRepository: WalletsRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        address: String,
        promoCode: String,
        payout: PromoPayout?,
    ): Either<ActivatePromoCodeError, PromoCodeActivation> = walletsRepository.activatePromoCode(
        userWalletId = userWalletId,
        promoCode = promoCode,
        address = address,
        payout = payout,
    )
}