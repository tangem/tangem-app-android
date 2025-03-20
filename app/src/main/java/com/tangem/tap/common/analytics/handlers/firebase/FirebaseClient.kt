package com.tangem.tap.common.analytics.handlers.firebase

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.recordException
import com.google.firebase.ktx.Firebase
import com.tangem.core.analytics.api.ExceptionLogger
import com.tangem.core.analytics.api.EventLogger

/**
[REDACTED_AUTHOR]
 */
interface FirebaseAnalyticsClient : EventLogger, ExceptionLogger

internal class FirebaseClient : FirebaseAnalyticsClient {

    private val fbAnalytics = Firebase.analytics
    private val fbCrashlytics = Firebase.crashlytics

    private val eventConverter = FirebaseAnalyticsEventConverter()

    override fun logEvent(event: String, params: Map<String, String>) {
        fbAnalytics.logEvent(
            eventConverter.convertEventName(event),
            eventConverter.convertEventParams(params).toBundle(),
        )
    }

    // TODO [REDACTED_TASK_KEY] fix recordException usages in the app
    override fun logException(error: Throwable, params: Map<String, String>) {
        fbCrashlytics.recordException(error) {
            eventConverter.convertEventParams(params)
                .forEach { key(it.key, it.value) }
        }
    }

    private fun Map<String, String>.toBundle(): Bundle = bundleOf(*this.toList().toTypedArray())
}