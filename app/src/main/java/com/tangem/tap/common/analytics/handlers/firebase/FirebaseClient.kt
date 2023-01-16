package com.tangem.tap.common.analytics.handlers.firebase

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.tangem.core.analytics.api.ErrorEventLogger
import com.tangem.core.analytics.api.EventLogger

/**
 * Created by Anton Zhilenkov on 22/09/2022.
 */
interface FirebaseAnalyticsClient : EventLogger, ErrorEventLogger

internal class FirebaseClient : FirebaseAnalyticsClient {

    private val fbAnalytics = Firebase.analytics
    private val fbCrashlytics = Firebase.crashlytics

    override fun logEvent(event: String, params: Map<String, String>) {
        fbAnalytics.logEvent(event, params.toBundle())
    }

    override fun logErrorEvent(error: Throwable, params: Map<String, String>) {
        params.forEach { fbCrashlytics.setCustomKey(it.key, it.value) }
        fbCrashlytics.recordException(error)
    }

    private fun Map<String, String>.toBundle(): Bundle = bundleOf(*this.toList().toTypedArray())

    companion object {
        const val ORDER_EVENT = FirebaseAnalytics.Event.PURCHASE
    }
}
