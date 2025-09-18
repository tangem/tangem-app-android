package com.tangem.domain.promo

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class ShouldShowPromoWalletUseCase(
    private val promoRepository: PromoRepository,
    private val settingsRepository: SettingsRepository,
) {

    operator fun invoke(userWalletId: UserWalletId, promoId: PromoId): Flow<Boolean> {
        return flow {
            emit(false)

            val promoFlow = promoRepository.isReadyToShowWalletPromo(userWalletId, promoId)
                .map { applyWalletFirstUsageCondition(promoId, it) }

            emitAll(promoFlow)
        }
    }

    private suspend fun applyWalletFirstUsageCondition(promoId: PromoId, isReady: Boolean): Boolean {
        if (!isReady) return false

        return when (promoId) {
            PromoId.Referral -> true
            PromoId.Sepa -> {
                val walletFirstUsageDate = settingsRepository.getWalletFirstUsageDate()
                if (walletFirstUsageDate == 0L) return false

                val currentDate = Calendar.getInstance().timeInMillis
                currentDate - walletFirstUsageDate > ONE_DAY_IN_MILLIS
            }
        }
    }
    suspend fun neverToShow(promoId: PromoId) = promoRepository.setNeverToShowWalletPromo(promoId)

    private companion object {
        const val ONE_DAY_IN_MILLIS = 1 * 24 * 60 * 60 * 1000L
    }
}