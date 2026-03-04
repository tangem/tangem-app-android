package com.tangem.tap.common.analytics.handlers.customerio

import timber.log.Timber

/**
 * Log client for Customer.io (used in debug mode).
 *
 * Logs all operations to Timber instead of sending them to Customer.io.
 */
internal class CustomerIoLogClient : CustomerIoAnalyticsClient {

    private var userId: String? = null

    override fun setUserId(userId: String) {
        this.userId = userId
        Timber.tag(CustomerIoAnalyticsHandler.ID).d("identify: userId=$userId")
    }

    override fun clearUserId() {
        Timber.tag(CustomerIoAnalyticsHandler.ID).d("clearIdentify: previous userId=$userId")
        this.userId = null
    }
}