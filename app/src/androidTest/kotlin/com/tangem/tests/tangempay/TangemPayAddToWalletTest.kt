package com.tangem.tests.tangempay

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.extensions.bringAppToForeground
import com.tangem.common.extensions.collapseAppByHomeButton
import com.tangem.common.extensions.restartApp
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.openTangemPayAddToWalletGuide
import com.tangem.scenarios.openTangemPayCardPage
import com.tangem.scenarios.openTangemPayCardPageFromPaymentAccount
import com.tangem.screens.tangempay.onTangemPayAddToWalletGuideScreen
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayAddToWalletTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"

    private val expectedBannerTitle = getResourceString(
        CoreResR.string.tangempay_card_details_open_wallet_notification_title,
    )
    private val expectedBannerSubtitle = getResourceString(
        CoreResR.string.tangempay_card_details_open_wallet_notification_subtitle,
    )
    private val expectedGuideSteps = listOf(
        CoreResR.string.tangempay_card_details_open_wallet_step_1,
        CoreResR.string.tangempay_card_details_open_wallet_step_2,
        CoreResR.string.tangempay_card_details_open_wallet_step_3,
        CoreResR.string.tangempay_card_details_open_wallet_step_4,
        CoreResR.string.tangempay_card_details_open_wallet_step_5,
    ).map(::getResourceString)

    @AllureId("9542")
    @DisplayName("Tangem Pay: 'Add to Google Pay' banner opens the guide with card requisites and instruction")
    @Test
    fun addToWalletBannerOpensGooglePayGuideTest() {
        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = { resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Assert 'Add to wallet' banner is displayed") {
                flakySafely { onTangemPayCardPageScreen { addToWalletBanner.assertIsDisplayed() } }
            }
            step("Assert banner title is '$expectedBannerTitle'") {
                onTangemPayCardPageScreen { addToWalletBannerTitle.assertTextEquals(expectedBannerTitle) }
            }
            step("Assert banner subtitle is '$expectedBannerSubtitle'") {
                onTangemPayCardPageScreen { addToWalletBannerSubtitle.assertTextEquals(expectedBannerSubtitle) }
            }
            step("Assert banner 'Close' button is displayed") {
                onTangemPayCardPageScreen { addToWalletBannerCloseButton.assertIsDisplayed() }
            }
            step("Open Add to wallet guide from the banner") { openTangemPayAddToWalletGuide() }
            step("Assert guide card requisites block is displayed") {
                flakySafely { onTangemPayAddToWalletGuideScreen { cardNumberShort.assertIsDisplayed() } }
                onTangemPayAddToWalletGuideScreen { showDetailsButton.assertIsDisplayed() }
            }
            step("Assert guide instruction title is displayed") {
                onTangemPayAddToWalletGuideScreen { title.assertIsDisplayed() }
            }
            expectedGuideSteps.forEachIndexed { index, stepText ->
                step("Assert guide instruction step ${index + 1} '$stepText' is displayed") {
                    onTangemPayAddToWalletGuideScreen { stepWithText(stepText).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9574")
    @DisplayName("Tangem Pay: dismissed 'Add to Google Pay' banner stays hidden after collapse and restart")
    @Test
    fun dismissedAddToWalletBannerStaysHiddenTest() {
        val packageName = getTargetContext().packageName

        setupHooks(
            additionalBeforeSection = { resetWireMockScenarios() },
            additionalAfterSection = { resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to state: '$eligibilityState'") {
                setWireMockScenarioState(scenarioName = TANGEM_PAY_ELIGIBILITY_SCENARIO, state = eligibilityState)
            }
            step("Open Tangem Pay card page") { openTangemPayCardPage() }
            step("Assert 'Add to wallet' banner is displayed") {
                flakySafely { onTangemPayCardPageScreen { addToWalletBanner.assertIsDisplayed() } }
            }
            step("Click on banner 'Close' button") {
                onTangemPayCardPageScreen { addToWalletBannerCloseButton.performClick() }
            }
            step("Assert 'Add to wallet' banner is not displayed") {
                flakySafely { onTangemPayCardPageScreen { addToWalletBanner.assertDoesNotExist() } }
            }
            step("Press 'Home' to collapse the app") { collapseAppByHomeButton() }
            step("Return to the app") { bringAppToForeground(packageName) }
            step("Assert 'Add to wallet' banner is not displayed after returning to the app") {
                flakySafely { onTangemPayCardPageScreen { moreButton.assertIsDisplayed() } }
                onTangemPayCardPageScreen { addToWalletBanner.assertDoesNotExist() }
            }
            // Decompose restores its child stack from saved state, so a relaunch lands back on Payment account.
            step("Force-close and re-launch the app") { restartApp(packageName) }
            step("Open Tangem Pay card page again") { openTangemPayCardPageFromPaymentAccount() }
            step("Assert 'Add to wallet' banner is not displayed after restart") {
                onTangemPayCardPageScreen { addToWalletBanner.assertDoesNotExist() }
            }
        }
    }
}