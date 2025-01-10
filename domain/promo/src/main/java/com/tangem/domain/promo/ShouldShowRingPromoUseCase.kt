package com.tangem.domain.promo

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

class ShouldShowRingPromoUseCase(private val promoRepository: PromoRepository) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> {
        return promoRepository.isReadyToShowRingPromo(userWalletId)
    }

    suspend fun neverToShow() = promoRepository.setNeverToShowRingPromo()
}