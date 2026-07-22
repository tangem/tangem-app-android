package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_KYC_STATUS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_SHORT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.tangempay.onTangemPayKycSheet
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayOnboardingScreen
import com.tangem.core.res.R as CoreResR
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayOnboardingKycTest : BaseTestCase() {

    @AllureId("9630")
    @DisplayName("Tangem Pay: existing customer authorization shows the tile on Main")
    @Test
    fun existingUserAuthorizationShowsTangemPayTileOnMainTest() {
        val paeraCustomerState = "PaeraCustomer"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$paeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, paeraCustomerState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert Tangem Pay tile is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { mainScreenTile.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9632")
    @DisplayName("Tangem Pay: Get Tangem Pay banner is shown on Main for the BANNER channel")
    @Test
    fun getTangemPayBannerShownOnMainWhenBannerChannelReceivedTest() {
        val eligibilityStartedState = "Started"
        val channelsBannerState = "Banner"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO)
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert 'Get Tangem Pay' banner is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { getTangemPayBanner.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9673")
    @DisplayName("Tangem Pay: onboarding opens from app Details for the DETAILS channel")
    @Test
    fun onboardingOpensFromAppDetailsWhenDetailsChannelReceivedTest() {
        val eligibilityStartedState = "Started"
        val channelsBannerAndDetailsState = "BannerAndDetails"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO)
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerAndDetailsState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerAndDetailsState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Click on 'More' button") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Assert 'Get Tangem Pay' row is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onDetailsScreen { getTangemPayRow.assertIsDisplayed() }
                }
            }
            step("Click on 'Get Tangem Pay' row") {
                onDetailsScreen { getTangemPayRow.clickWithAssertion() }
            }
            step("Assert Tangem Pay onboarding screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayOnboardingScreen { title.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9643")
    @DisplayName("Tangem Pay: KYC in progress sheet closes and the status remains on Main")
    @Test
    fun kycInProgressSheetClosesAndStatusRemainsOnMainTest() {
        val paeraCustomerState = "PaeraCustomer"
        val kycInProgressState = "InProgress"
        val kycInProgressText = getResourceString(CoreResR.string.tangempay_kyc_in_progress)
        val viewStatusText = getResourceString(CoreResR.string.tangempay_kyc_in_progress_notification_button)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO)
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$paeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, paeraCustomerState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycInProgressState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycInProgressState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert Tangem Pay tile with '$kycInProgressText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
                }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert KYC in progress sheet is displayed") {
                onTangemPayKycSheet {
                    title(kycInProgressText).assertIsDisplayed()
                    primaryButtonWithText(viewStatusText).assertIsDisplayed()
                }
            }
            step("Click on 'Close' button") {
                onTangemPayKycSheet { closeButton.clickWithAssertion() }
            }
            step("Assert KYC in progress sheet is not displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_SHORT) {
                    onTangemPayKycSheet { primaryButtonWithText(viewStatusText).assertDoesNotExist() }
                }
            }
            step("Assert Tangem Pay tile with '$kycInProgressText' is still displayed") {
                onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
            }
        }
    }

    @AllureId("9517")
    @DisplayName("Tangem Pay: KYC rejected status shown on Main and in the sheet")
    @Test
    fun kycRejectedStatusShownOnMainAndInSheetTest() {
        val paeraCustomerState = "PaeraCustomer"
        val kycDeclinedState = "Declined"
        val kycRejectedTileText = getResourceString(CoreResR.string.tangempay_kyc_has_failed)
        val rejectedSheetTitle = getResourceString(CoreResR.string.tangempay_kyc_rejected)
        val goToSupportText = getResourceString(CoreResR.string.tangempay_go_to_support)

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO)
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$paeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, paeraCustomerState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycDeclinedState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycDeclinedState)
            }
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert Tangem Pay tile with '$kycRejectedTileText' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycRejectedTileText).assertIsDisplayed() }
                }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert KYC rejected sheet is displayed") {
                onTangemPayKycSheet {
                    title(rejectedSheetTitle).assertIsDisplayed()
                    primaryButtonWithText(goToSupportText).assertIsDisplayed()
                }
            }
        }
    }
}