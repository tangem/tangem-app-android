package com.tangem.tap.features.home.redux

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.entities.IndeterminateProgressButton
import org.rekotlin.Action

sealed class HomeAction : Action {

    object OnCreate : HomeAction()
    object Init : HomeAction()

    data class ReadCard(
        val analyticsEvent: AnalyticsEvent? = Basic.CardWasScanned(AnalyticsParam.ScannedFrom.Introduction),
    ) : HomeAction()

    data class ScanInProgress(val scanInProgress: Boolean) : HomeAction()
    data class GoToShop(val userCountryCode: String?) : HomeAction()

    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()
}
