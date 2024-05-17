package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.PromoSettingsRepository
import kotlinx.coroutines.flow.Flow

class ShouldShowTravalaPromoWalletUseCase(private val promoSettingsRepository: PromoSettingsRepository) {

    operator fun invoke(): Flow<Boolean> = promoSettingsRepository.isReadyToShowWalletTravalaPromo()

    suspend fun neverToShow() = promoSettingsRepository.setNeverToShowWalletTravalaPromo()
}
