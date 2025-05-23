package com.tangem.tap.common.analytics.handlers.firebase

import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.recordException
import com.google.firebase.ktx.Firebase
import com.tangem.core.analytics.api.ExceptionLogger
import com.tangem.core.analytics.api.EventLogger
import com.tangem.core.analytics.api.UserIdHolder

/**
* [REDACTED_AUTHOR]
 */
interface FirebaseAnalyticsClient : EventLogger, ExceptionLogger, UserIdHolder

internal class FirebaseClient : FirebaseAnalyticsClient {

    private val fbAnalytics = Firebase.analytics
    private val fbCrashlytics = Firebase.crashlytics

    private val eventConverter = FirebaseAnalyticsEventConverter()

    override fun setUserId(userId: String) {
        Firebase.analytics.setUserId(userId)
    }

    override fun clearUserId() {
        Firebase.analytics.setUserId(null)
    }

    override fun logEvent(event: String, params: Map<String, String>) {
        fbAnalytics.logEvent(
            eventConverter.convertEventName(event),
            eventConverter.convertEventParams(params).toBundle(),
        )
    }

    override fun logException(error: Throwable, params: Map<String, String>) {
        fbCrashlytics.recordException(error) {
            eventConverter.convertEventParams(params)
                .forEach { key(it.key, it.value) }
        }
    }

    private fun Map<String, String>.toBundle(): Bundle = bundleOf(*this.toList().toTypedArray())
}
