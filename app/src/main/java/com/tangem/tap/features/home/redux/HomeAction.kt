package com.tangem.tap.features.home.redux

import androidx.lifecycle.LifecycleCoroutineScope
import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.Basic
import com.tangem.tap.common.entities.IndeterminateProgressButton
import org.rekotlin.Action

sealed class HomeAction : Action {

    object OnCreate : HomeAction()
    object Init : HomeAction()

    data class InsertStory(val position: Int, val story: Stories) : HomeAction()

    /**
     * Action for scanning card
     *
     * @property analyticsEvent          analytics event
     * @property lifecycleCoroutineScope lifecycle scope. It will be canceled when lifecycle-aware component is
     * destroyed.
     */
    data class ReadCard(
        val analyticsEvent: AnalyticsEvent? = Basic.CardWasScanned(AnalyticsParam.ScannedFrom.Introduction),
        val lifecycleCoroutineScope: LifecycleCoroutineScope,
    ) : HomeAction()

    data class ScanInProgress(val scanInProgress: Boolean) : HomeAction()
    data class GoToShop(val userCountryCode: String?) : HomeAction()

    data class ChangeScanCardButtonState(val state: IndeterminateProgressButton) : HomeAction()
}