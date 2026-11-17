package com.tangem.domain.polymarket.sessionkey

import java.time.Duration
import java.time.Instant

private const val LONG_DISMISSAL_DAYS = 30L
private const val REACTIVATE_DISMISSAL_DAYS = 1L

enum class PolymarketFastTradingBannerKind(val dismissalPeriod: Duration) {
    ACTIVATE(dismissalPeriod = Duration.ofDays(LONG_DISMISSAL_DAYS)),
    REACTIVATE(dismissalPeriod = Duration.ofDays(REACTIVATE_DISMISSAL_DAYS)),
    EXTEND(dismissalPeriod = Duration.ofDays(LONG_DISMISSAL_DAYS)),
    ;

    fun isDismissed(dismissedAt: Instant?, now: Instant): Boolean {
        if (dismissedAt == null) return false
        return now.isBefore(dismissedAt.plus(dismissalPeriod))
    }
}