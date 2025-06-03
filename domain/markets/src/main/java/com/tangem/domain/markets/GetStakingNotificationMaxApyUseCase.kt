package com.tangem.domain.markets

import android.icu.util.Calendar
import com.tangem.domain.markets.repositories.MarketsTokenRepository
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.settings.repositories.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

class GetStakingNotificationMaxApyUseCase(
    private val settingsRepository: SettingsRepository,
    private val promoRepository: PromoRepository,
    private val marketsTokenRepository: MarketsTokenRepository,
) {

    suspend operator fun invoke(): Flow<BigDecimal?> {
        val hideClickedFlow = promoRepository.isMarketsStakingNotificationHideClicked()
        val walletFirstUsageDate = settingsRepository.getWalletFirstUsageDate()
        val currentDate = Calendar.getInstance().timeInMillis

        return combine(
            flow = hideClickedFlow,
            flow2 = marketsTokenRepository.getMaxApy(),
        ) { hideClicked, maxApy ->
            val showStakingNotification = if (!hideClicked && walletFirstUsageDate != 0L) {
                currentDate - walletFirstUsageDate > TWO_WEEKS_IN_MILLIS
            } else {
                false
            }

            maxApy.takeIf { showStakingNotification }
        }
    }

    private companion object {
        const val TWO_WEEKS_IN_MILLIS = 14 * 24 * 60 * 60 * 1000L
    }
}