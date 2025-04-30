package com.tangem.domain.promo

import com.tangem.domain.promo.models.PromoId
import kotlinx.coroutines.flow.Flow

class ShouldShowPromoTokenUseCase(private val promoRepository: PromoRepository) {

    operator fun invoke(promoId: PromoId): Flow<Boolean> = promoRepository.isReadyToShowTokenPromo(promoId)

    suspend fun neverToShow(promoId: PromoId) = promoRepository.setNeverToShowTokenPromo(promoId)
}