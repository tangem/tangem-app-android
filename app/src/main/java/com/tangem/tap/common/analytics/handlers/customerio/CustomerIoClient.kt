package com.tangem.tap.common.analytics.handlers.customerio

import android.app.Application
import com.tangem.utils.logging.TangemLogger
import io.customer.messagingpush.ModuleMessagingPushFCM
import io.customer.sdk.CustomerIO
import io.customer.sdk.CustomerIOBuilder
import io.customer.sdk.data.model.Region

/**
 * Real Customer.io SDK client.
 *
 * Initializes the SDK with the given CDP API key and provides:
 * - User identification (identify / clearIdentify)
 *
 * Auto-tracking of application lifecycle events is disabled since it is not needed.
 * Auto-tracking of screen views is disabled.
 */
internal class CustomerIoClient(
    application: Application,
    cdpApiKey: String,
) : CustomerIoAnalyticsClient {

    init {
        CustomerIOBuilder(
            applicationContext = application,
            cdpApiKey = cdpApiKey,
        )
            .region(Region.EU)
            .trackApplicationLifecycleEvents(false)
            .autoTrackActivityScreens(false)
            .addCustomerIOModule(ModuleMessagingPushFCM())
            .build()

        TangemLogger.d("CustomerIO SDK initialized")
    }

    override fun setUserId(userId: String) {
        CustomerIO.instance().identify(userId = userId)
    }

    override fun clearUserId() {
        CustomerIO.instance().clearIdentify()
    }
}