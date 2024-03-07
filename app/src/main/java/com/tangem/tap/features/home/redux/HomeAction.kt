package com.tangem.tap.features.home.redux

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import kotlinx.coroutines.CoroutineScope
import org.rekotlin.Action

sealed class HomeAction : Action {

    object OnCreate : HomeAction()
    object Init : HomeAction()

    data class InsertStory(val position: Int, val story: Stories) : HomeAction()

    /**
     * Action for scanning card
     *
     * @property analyticsEvent analytics event
     * @property scope          lifecycle scope. It will be canceled when lifecycle-aware component is destroyed
     */
    data class ReadCard(
        val analyticsEvent: AnalyticsEvent? = Basic.CardWasScanned(AnalyticsParam.ScannedFrom.Introduction),
        val scope: CoroutineScope,
    ) : HomeAction()

    data class ScanInProgress(val scanInProgress: Boolean) : HomeAction()
    data class GoToShop(val userCountryCode: String?) : HomeAction()

    data class UpdateCountryCode(val userCountryCode: String) : HomeAction()
}