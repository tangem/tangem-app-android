package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.PromoSettingsRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

class ShouldShowRingPromoUseCase(private val promoSettingsRepository: PromoSettingsRepository) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Boolean> {
        return promoSettingsRepository.isReadyToShowRingPromo(userWalletId)
    }

    suspend fun neverToShow() = promoSettingsRepository.setNeverToShowRingPromo()
}
