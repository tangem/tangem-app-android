package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.utils.getWireMockRequestCount

private const val ELIGIBILITY_CHANNELS_ENDPOINT = "/bff-v2/v1/eligibility/channels"

private const val BANNER_DISMISSAL_KEY = "uitest.tangempay.banner_dismissal"

fun enableTangemPayBannerDismissal() {
    System.setProperty(BANNER_DISMISSAL_KEY, "1")
}

fun disableTangemPayBannerDismissal() {
    System.clearProperty(BANNER_DISMISSAL_KEY)
}

fun getTangemPayEligibilityChannelsRequestCount(): Int =
    getWireMockRequestCount(method = "GET", urlPathPattern = ELIGIBILITY_CHANNELS_ENDPOINT)

fun BaseTestCase.assertTangemPayEligibilityChannelsRequested(countBefore: Int) {
    awaitSuccess {
        if (getTangemPayEligibilityChannelsRequestCount() <= countBefore) {
            throw AssertionError("The app should request $ELIGIBILITY_CHANNELS_ENDPOINT")
        }
    }
}