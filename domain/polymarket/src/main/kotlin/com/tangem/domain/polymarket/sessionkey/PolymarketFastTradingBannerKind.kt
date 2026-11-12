package com.tangem.domain.polymarket.sessionkey

import java.time.Duration
import java.time.Instant

private const val ACTIVATE_DISMISSAL_DAYS = 30L
private const val EXTEND_DISMISSAL_DAYS = 1L

enum class PolymarketFastTradingBannerKind(val dismissalPeriod: Duration) {
    ACTIVATE(dismissalPeriod = Duration.ofDays(ACTIVATE_DISMISSAL_DAYS)),
    EXTEND(dismissalPeriod = Duration.ofDays(EXTEND_DISMISSAL_DAYS)),
    ;

    fun isDismissed(dismissedAt: Instant?, now: Instant): Boolean {
        if (dismissedAt == null) return false
        return now.isBefore(dismissedAt.plus(dismissalPeriod))
    }
}