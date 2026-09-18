package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_KYC_STATUS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_WALLET_CREATION
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.domain.appsflyer.AppsFlyerDeeplink
import com.tangem.domain.appsflyer.repository.AppsFlyerRepository
import com.tangem.scenarios.exitTangemPayKycScreen
import com.tangem.scenarios.openMainScreenWithNewHotWallet
import com.tangem.screens.onMainScreen
import com.tangem.screens.onIntroductionScreen
import com.tangem.screens.tangempay.onTangemPayHotWalletOnboardingScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayOnboardingScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import kotlinx.coroutines.runBlocking
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class TangemPayHotWalletOnboardingTest : BaseTestCase() {

    @Inject
    lateinit var appsFlyerRepository: AppsFlyerRepository

    private val kycInProgressText = getResourceString(CoreResR.string.tangempay_kyc_in_progress)

    @AllureId("10666")
    @DisplayName("Tangem Pay: card issuing starts from the Main banner in a freshly created hot wallet")
    @Test
    fun cardIssuingStartsFromMainBannerTest() {
        val eligibilityStartedState = "Started"
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val channelsBannerState = "Banner"
        val kycInProgressState = "InProgress"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycInProgressState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycInProgressState)
            }
            step("Create a new mobile wallet and open 'Main' screen") {
                openMainScreenWithNewHotWallet()
            }
            step("Assert 'Get Tangem Pay' banner is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMainScreen { getTangemPayBanner.assertIsDisplayed() }
                }
            }
            step("Click on 'Learn more' button on the 'Get Tangem Pay' banner") {
                onMainScreen { getTangemPayBannerLearnMoreButton.clickWithAssertion() }
            }
            step("Assert Tangem Pay onboarding screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayOnboardingScreen { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Get card' button") {
                onTangemPayOnboardingScreen { getCardButton.clickWithAssertion() }
            }
            // The backend turns the wallet into a Paera customer once the offer is accepted.
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Leave the KYC screen") {
                exitTangemPayKycScreen()
            }
            step("Assert Tangem Pay tile with '$kycInProgressText' is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("10413")
    @DisplayName("Tangem Pay: card issuing starts from the mobile onboarding opened automatically by a deep link")
    @Test
    fun cardIssuingStartsFromMobileOnboardingDeepLinkTest() {
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val kycInProgressState = "InProgress"
        val mobileOnboardingDeepLink = AppsFlyerDeeplink.TangemPayMobileOnboarding.deepLinkValue

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_KYC_STATUS_SCENARIO' to '$kycInProgressState'") {
                setWireMockScenarioState(TANGEM_PAY_KYC_STATUS_SCENARIO, kycInProgressState)
            }
            step("Assert 'Introduction' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onIntroductionScreen { createWalletButton.assertIsDisplayed() }
                }
            }
            // The AppsFlyer SDK never delivers its callback in tests, so the deep link it would have stored
            // is written straight to the repository the app reads it back from.
            step("Store the AppsFlyer deep link '$mobileOnboardingDeepLink'") {
                runBlocking { appsFlyerRepository.storeNavigationDeeplink(mobileOnboardingDeepLink) }
            }
            step("Assert Tangem Pay mobile onboarding screen opens without any tap") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayHotWalletOnboardingScreen {
                        title.assertIsDisplayed()
                        setupWalletBlockTitle.assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Get card' button") {
                onTangemPayHotWalletOnboardingScreen { getCardButton.clickWithAssertion() }
            }
            step("Leave the KYC screen") {
                // 'Get card' creates the mobile wallet before handing over to KYC.
                exitTangemPayKycScreen(timeoutMillis = WAIT_UNTIL_TIMEOUT_WALLET_CREATION)
            }
            step("Assert Tangem Pay tile with '$kycInProgressText' is displayed on Main") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { tileWithSubtitle(kycInProgressText).assertIsDisplayed() }
                }
            }
        }
    }
}