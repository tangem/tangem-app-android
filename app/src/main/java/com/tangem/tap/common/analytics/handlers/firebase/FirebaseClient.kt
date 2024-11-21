package com.tangem.tap.common.analytics.handlers.firebase

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.tangem.core.analytics.api.ErrorEventLogger
import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.models.EventValue

/**
[REDACTED_AUTHOR]
 */
interface FirebaseAnalyticsClient : EventLogger, ErrorEventLogger

internal class FirebaseClient : FirebaseAnalyticsClient {

    private val fbAnalytics = Firebase.analytics
    private val fbCrashlytics = Firebase.crashlytics

    override fun logEvent(event: String, params: Map<String, EventValue>) {
        fbAnalytics.logEvent(event, params.toBundle())
    }

    override fun logErrorEvent(error: Throwable, params: Map<String, EventValue>) {
        params.forEach { fbCrashlytics.setCustomKey(it.key, it.value.toString()) } // TODO analytics
        fbCrashlytics.recordException(error)
    }

    private fun Map<String, EventValue>.toBundle(): Bundle = bundleOf(*this.toList().toTypedArray()) // TODO analytics
}