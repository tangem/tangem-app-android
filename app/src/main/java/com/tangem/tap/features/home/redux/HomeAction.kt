package com.tangem.tap.features.home.redux

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.entities.IndeterminateProgressButton
import org.rekotlin.Action

sealed class HomeAction : Action {

    object OnCreate : HomeAction()
    object Init : HomeAction()

    data class InsertStory(val position: Int, val story: Stories) : HomeAction()

    data class ReadCard(
        val analyticsEvent: AnalyticsEvent? = Basic.CardWasScanned(AnalyticsParam.ScannedFrom.Introduction),
    ) : HomeAction()

    data class ScanInProgress(val scanInProgress: Boolean) : HomeAction()
    data class GoToShop(val userCountryCode: String?) : HomeAction()

    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()
}
