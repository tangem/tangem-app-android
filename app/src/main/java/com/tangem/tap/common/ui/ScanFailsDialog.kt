package com.tangem.tap.common.ui

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.tangem.core.analytics.Analytics
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.redux.StateDialog
import com.tangem.tap.common.analytics.events.ScanFailsDialogAnalytics
import com.tangem.tap.common.extensions.dispatchDialogHide
import com.tangem.tap.common.extensions.dispatchOpenUrl
import com.tangem.tap.common.feedback.ScanFailsEmail
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.home.LocaleRegionProvider
import com.tangem.tap.features.home.RUSSIA_COUNTRY_CODE
import com.tangem.tap.store
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
internal object ScanFailsDialog {

    private const val HOW_TO_SCAN_RU_LINK = "https://tangem.com/ru/blog/post/scan-tangem-card/"
    private const val HOW_TO_SCAN_LINK = "https://tangem.com/en/blog/post/scan-tangem-card/"

    fun create(context: Context, source: StateDialog.ScanFailsSource, onTryAgain: (() -> Unit)? = null): AlertDialog {
        return AlertDialog.Builder(context, R.style.CustomMaterialDialog).apply {
            val customView = View.inflate(context, R.layout.dialog_scan_fails, null)
            val sourceAnalytics = when (source) {
                StateDialog.ScanFailsSource.MAIN -> AnalyticsParam.ScreensSources.Main
                StateDialog.ScanFailsSource.SIGN_IN -> AnalyticsParam.ScreensSources.SignIn
                StateDialog.ScanFailsSource.SETTINGS -> AnalyticsParam.ScreensSources.Settings
                StateDialog.ScanFailsSource.INTRO -> AnalyticsParam.ScreensSources.Intro
            }
            val tryAgainBtn: TextView? = customView.findViewById(R.id.try_again_button)
            if (onTryAgain != null) {
                tryAgainBtn?.isVisible = true
                tryAgainBtn?.setOnClickListener {
                    store.dispatchDialogHide()
                    Analytics.send(
                        ScanFailsDialogAnalytics(
                            button = ScanFailsDialogAnalytics.Buttons.TRY_AGAIN,
                            source = sourceAnalytics,
                        ),
                    )
                    onTryAgain()
                }
            } else {
                tryAgainBtn?.isVisible = false
            }
            customView.findViewById<TextView>(R.id.how_to_scan_button)?.setOnClickListener {
                Analytics.send(
                    ScanFailsDialogAnalytics(
                        button = ScanFailsDialogAnalytics.Buttons.HOW_TO_SCAN,
                        source = sourceAnalytics,
                    ),
                )
                val locale = LocaleRegionProvider().getRegion()
                val link = if (locale.lowercase() == RUSSIA_COUNTRY_CODE) HOW_TO_SCAN_RU_LINK else HOW_TO_SCAN_LINK
                store.dispatchOpenUrl(link)
            }
            customView.findViewById<TextView>(R.id.request_support_button)?.setOnClickListener {
                Analytics.send(Basic.ButtonSupport(sourceAnalytics))
                store.dispatch(GlobalAction.SendEmail(feedbackData = ScanFailsEmail(), scanResponse = null))
            }
            customView.findViewById<TextView>(R.id.cancel_button)?.setOnClickListener {
                store.dispatchDialogHide()
            }
            setView(customView)
            setOnDismissListener { store.dispatchDialogHide() }
        }.create()
    }
}