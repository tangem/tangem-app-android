package com.tangem.domain.promo

import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

class ShouldShowRingPromoUseCase(private val promoSettingsRepository: PromoRepository) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> {
        return promoSettingsRepository.isReadyToShowRingPromo(userWalletId)
    }

    suspend fun neverToShow() = promoSettingsRepository.setNeverToShowRingPromo()
}