package com.tangem.feature.tokendetails.domain

import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning

internal class CurrencyWarningsStabilizer {

    private var isOutdatedDataWarningHeld = false
    private var handledWarnings: Set<CryptoCurrencyWarning>? = null

    /** Returns warnings to display, or null when they are the same as the last displayed ones */
    fun stabilize(warnings: Set<CryptoCurrencyWarning>, totalSource: StatusSource): Set<CryptoCurrencyWarning>? {
        val outdatedDataWarning = CryptoCurrencyWarning.UsedOutdatedDataWarning

        isOutdatedDataWarningHeld = when {
            outdatedDataWarning in warnings -> true
            totalSource == StatusSource.ACTUAL -> false
            else -> isOutdatedDataWarningHeld
        }

        val stabilized = if (isOutdatedDataWarningHeld) warnings + outdatedDataWarning else warnings

        return stabilized.takeIf { it != handledWarnings }?.also { handledWarnings = it }
    }

    fun invalidate() {
        handledWarnings = null
    }
}