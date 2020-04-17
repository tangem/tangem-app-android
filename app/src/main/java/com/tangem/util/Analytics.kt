package com.tangem.util

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.tangem.data.dp.PrefsManager
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext

enum class AnalyticsEvent(val event: String) {
    CARD_IS_SCANNED("card_is_scanned"),
    TRANSACTION_IS_SENT("transaction_is_sent"),
    READY_TO_SCAN("ready_to_scan"),
    READY_TO_SIGN("ready_to_sign"),
    SIGNED("signed"),
    ANALYTICS_TURNED_OFF("analytics_turned_off"),
    CARD_TAP_USER_TIMER("CardTapUserTimer"),
    ;
}

enum class AnalyticsParam(val param: String) {
    BLOCKCHAIN("blockchain"),
    BATCH_ID("batch_id"),
    FIRMWARE("firmware"),
    ;
}

class Analytics {
    companion object {
        fun setCardData(ctx: TangemContext): Bundle {
            return bundleOf(
                    AnalyticsParam.BLOCKCHAIN.param to ctx.blockchainName,
                    AnalyticsParam.BATCH_ID.param to ctx.card.batch,
                    AnalyticsParam.FIRMWARE.param to ctx.card.firmwareVersion
            )
        }

        fun isEnabled(): Boolean {
            return PrefsManager.getInstance().getSettingsBoolean(R.string.pref_analytics, true)
        }

        fun setFirebaseEnabled(context: Context, enable: Boolean = isEnabled()) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enable)
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(
                    enable && !BuildConfig.DEBUG
            )
            FirebasePerformance.getInstance().isPerformanceCollectionEnabled = enable
        }
    }
}
