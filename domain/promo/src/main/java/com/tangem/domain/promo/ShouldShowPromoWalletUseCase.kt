package com.tangem.domain.promo

import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

class ShouldShowPromoWalletUseCase(private val promoRepository: PromoRepository) {

    operator fun invoke(userWalletId: UserWalletId, promoId: PromoId): Flow<Boolean> {
        return promoRepository.isReadyToShowWalletPromo(userWalletId, promoId)
    }

    suspend fun neverToShow(promoId: PromoId) = promoRepository.setNeverToShowWalletPromo(promoId)
}