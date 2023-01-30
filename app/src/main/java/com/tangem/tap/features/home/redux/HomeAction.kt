package com.tangem.tap.features.home.redux

import com.tangem.core.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.events.IntroductionProcess
import com.tangem.tap.common.entities.IndeterminateProgressButton
import org.rekotlin.Action

sealed class HomeAction : Action {

    object Init : HomeAction()

    data class ReadCard(
        val analyticsEvent: AnalyticsEvent? = IntroductionProcess.CardWasScanned(),
    ) : HomeAction()

    data class ScanInProgress(val scanInProgress: Boolean) : HomeAction()
    data class GoToShop(val userCountryCode: String?) : HomeAction()

    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()
}
