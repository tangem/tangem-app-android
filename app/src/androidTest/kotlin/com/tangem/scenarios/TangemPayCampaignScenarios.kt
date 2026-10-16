package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.findWireMockRequestBodies
import com.tangem.screens.tangempay.onTangemPayCampaignScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import io.qameta.allure.kotlin.Allure.step
import org.json.JSONObject

private const val DEEPLINK_VALIDATE_ENDPOINT = "/bff-v2/v1/deeplink/validate"
private const val ORDER_ENDPOINT = "/bff-v2/v1/order"
private const val ONBOARDING_ORDER_FROM_API_KEY = "uitest.tangempay.onboarding_order_from_api"

fun serveTangemPayOnboardingOrderFromWireMock() {
    System.setProperty(ONBOARDING_ORDER_FROM_API_KEY, "1")
}

fun restoreTangemPayMockOnboardingOrder() {
    System.clearProperty(ONBOARDING_ORDER_FROM_API_KEY)
}

fun countTangemPayDeeplinkValidations(link: String): Int =
    findWireMockRequestBodies("POST", DEEPLINK_VALIDATE_ENDPOINT).count { body ->
        JSONObject(body).optString("link") == link
    }

fun countTangemPayCampaignOrders(campaignKey: String): Int =
    findWireMockRequestBodies("POST", ORDER_ENDPOINT).count { body ->
        JSONObject(body).optJSONObject("data")?.optString("deeplink_value") == campaignKey
    }

fun BaseTestCase.assertTangemPayCampaignLandingContent() {
    step("Assert campaign landing title is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayCampaignScreen { landingTitle.assertIsDisplayed() }
        }
    }
    step("Assert 'Unrivaled privacy' block is displayed") {
        onTangemPayCampaignScreen {
            privacyBlockTitle.assertIsDisplayed()
            privacyBlockDescription.assertIsDisplayed()
        }
    }
    step("Assert 'Cashback up to 2%' block is displayed") {
        onTangemPayCampaignScreen {
            cashbackBlockTitle.assertIsDisplayed()
            cashbackBlockDescription.assertIsDisplayed()
        }
    }
    step("Assert 'Spend your assets anywhere' block is displayed") {
        onTangemPayCampaignScreen {
            spendBlockTitle.assertIsDisplayed()
            spendBlockDescription.assertIsDisplayed()
        }
    }
    step("Assert the blocks are in the 'Privacy', 'Cashback', 'Spend' order") {
        onTangemPayCampaignScreen { assertBlocksAreInOrder() }
    }
    step("Assert 'Get card' button is displayed") {
        onTangemPayCampaignScreen { getCardButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.assertTangemPayCampaignIssueCardContent(issueFee: String) {
    step("Assert 'Issue special card' screen is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayCampaignScreen { issueCardTitle.assertIsDisplayed() }
        }
    }
    step("Assert 'Issue fee' equals '$issueFee'") {
        onTangemPayCampaignScreen {
            issueFeeTitle.assertIsDisplayed()
            issueFeeValue(issueFee).assertIsDisplayed()
        }
    }
    step("Assert 'Issue time' is 'Instant'") {
        onTangemPayCampaignScreen {
            issueTimeTitle.assertIsDisplayed()
            issueTimeInstantValue.assertIsDisplayed()
        }
    }
    step("Assert 'Issue' button and 'Close' button are displayed") {
        onTangemPayCampaignScreen {
            issueButton.assertIsDisplayed()
            closeButton.assertIsDisplayed()
        }
    }
}

fun BaseTestCase.refreshMainUntilTangemPayTileSubtitle(subtitle: String) {
    step("Refresh 'Main' until the Tangem Pay tile shows '$subtitle'") {
        // Pull-to-refresh is idempotent, so it is repeated until the new customer state lands.
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            pullToRefreshMainScreen()
            runCatching { onTangemPayMainScreen { tileWithSubtitle(subtitle).assertIsDisplayed() } }.isSuccess
        }
    }
}