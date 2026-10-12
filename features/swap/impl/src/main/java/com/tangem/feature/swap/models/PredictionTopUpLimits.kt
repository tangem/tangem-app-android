package com.tangem.feature.swap.models

import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.feature.swap.models.states.SwapNotificationUM
import java.math.BigDecimal

/**
 * @property minimum smallest amount the prediction account accepts, in units of the receive token
 * @property minimumText [minimum] in USD, as shown to the user
 * @property maximum amount the prediction account no longer accepts, in units of the receive token
 * @property maximumText [maximum] in USD, as shown to the user
 */
internal data class PredictionTopUpLimits(
    val minimum: BigDecimal,
    val minimumText: String,
    val maximum: BigDecimal,
    val maximumText: String,
) {
    companion object {
        val MAXIMUM_USD: BigDecimal = BigDecimal(50_000)
    }
}

internal fun PredictionTopUpLimits.warningFor(receiveAmount: BigDecimal): NotificationUM.Warning? = when {
    receiveAmount.signum() > 0 && receiveAmount < minimum -> {
        SwapNotificationUM.Warning.PredictionMinimumDeposit(amountText = minimumText)
    }
    receiveAmount >= maximum -> SwapNotificationUM.Warning.PredictionMaximumDeposit(amountText = maximumText)
    else -> null
}