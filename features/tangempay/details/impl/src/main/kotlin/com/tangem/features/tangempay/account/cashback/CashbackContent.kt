package com.tangem.features.tangempay.account.cashback

import com.tangem.domain.pay.model.CashbackDisplayMode
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents

internal data class CashbackRefreshState(val isFailed: Boolean = false, val isInProgress: Boolean = false)

internal sealed interface CashbackContent {

    data object Hidden : CashbackContent

    data class Widget(val cashback: TangemPayCashback) : CashbackContent

    data class MenuEntry(val cashback: TangemPayCashback) : CashbackContent

    data object DeactivationBanner : CashbackContent

    data class Unavailable(val isRetrying: Boolean) : CashbackContent
}

internal enum class CashbackImpression { Widget, SettingsButton, DeactivationBanner, Error }

internal fun resolveCashbackContent(
    summary: CashbackSummary?,
    refreshState: CashbackRefreshState,
    isDeactivationDismissed: Boolean,
): CashbackContent = when (summary) {
    null -> if (refreshState.isFailed) {
        CashbackContent.Unavailable(isRetrying = refreshState.isInProgress)
    } else {
        CashbackContent.Hidden
    }
    is CashbackSummary.Enabled -> when (summary.displayMode) {
        CashbackDisplayMode.FULL -> CashbackContent.Widget(summary.cashback)
        CashbackDisplayMode.ALT_BLOCK -> CashbackContent.MenuEntry(summary.cashback)
    }
    CashbackSummary.Deactivated -> if (isDeactivationDismissed) {
        CashbackContent.Hidden
    } else {
        CashbackContent.DeactivationBanner
    }
    CashbackSummary.Disabled,
    CashbackSummary.Unknown,
    -> CashbackContent.Hidden
}

internal fun CashbackContent.impression(): CashbackImpression? = when (this) {
    CashbackContent.Hidden -> null
    is CashbackContent.Widget -> CashbackImpression.Widget
    is CashbackContent.MenuEntry -> CashbackImpression.SettingsButton
    CashbackContent.DeactivationBanner -> CashbackImpression.DeactivationBanner
    is CashbackContent.Unavailable -> CashbackImpression.Error
}

internal fun CashbackImpression.toAnalyticsEvent(): TangemPayAnalyticsEvents = when (this) {
    CashbackImpression.Widget -> TangemPayAnalyticsEvents.Cashback.BannerShowed()
    CashbackImpression.SettingsButton -> TangemPayAnalyticsEvents.Cashback.ButtonInSettingsShowed()
    CashbackImpression.DeactivationBanner -> TangemPayAnalyticsEvents.Cashback.DeactivationBannerShowed()
    CashbackImpression.Error -> TangemPayAnalyticsEvents.Cashback.BannerErrorStateShowed()
}