package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.scenarios.addNewCardWallet
import com.tangem.scenarios.assertTangemPayEligibilityChannelsRequested
import com.tangem.scenarios.awaitTangemPayStatusOfSelectedWallet
import com.tangem.scenarios.disableTangemPayBannerDismissal
import com.tangem.scenarios.enableTangemPayBannerDismissal
import com.tangem.scenarios.getTangemPayEligibilityChannelsRequestCount
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openMainScreenWithTangemPayTile
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.tangempay.onTangemPayOnboardingScreen
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import com.tangem.utils.annotations.RemoveWithToggle
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class TangemPayEligibilityTest : BaseTestCase() {

    @Inject
    lateinit var userWalletsListRepository: UserWalletsListRepository

    @Inject
    lateinit var paymentAccountStatusFetcher: PaymentAccountStatusFetcher

    @Inject
    lateinit var paymentAccountStatusSupplier: PaymentAccountStatusSupplier

    @RemoveWithToggle(
        toggleName = "TWI_1841_HIDE_MARKETING_BANNERS_WHILE_SYSTEM_ENABLED",
        description = "Move the tests to a wallet without system cards, where the Main promo is shown",
    )
    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1841_HIDE_MARKETING_BANNERS_WHILE_SYSTEM_ENABLED.rawName to false,
    )

    @AllureId("9634")
    @DisplayName("Tangem Pay: no Get Tangem Pay banner on Main for an empty eligibility channels list")
    @Test
    fun getTangemPayBannerNotShownForEmptyChannelsTest() {
        val eligibilityStartedState = "Started"
        val channelsEmptyState = "Started"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsEmptyState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsEmptyState)
            }
            val channelsRequestsBefore = getTangemPayEligibilityChannelsRequestCount()
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert eligibility channels are requested") {
                assertTangemPayEligibilityChannelsRequested(channelsRequestsBefore)
            }
            step("Wait for Tangem Pay status 'Not available'") {
                awaitTangemPayStatusOfSelectedWallet(
                    userWalletsListRepository = userWalletsListRepository,
                    paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                    paymentAccountStatusSupplier = paymentAccountStatusSupplier,
                    expectedStatus = PaymentAccountStatusValue.Empty::class,
                )
            }
            step("Assert 'Get Tangem Pay' banner is not displayed") {
                onMainScreen { getTangemPayBanner.assertDoesNotExist() }
            }
        }
    }

    @AllureId("9635")
    @DisplayName("Tangem Pay: Get Tangem Pay stays in app Details after the Main banner is closed")
    @Test
    fun getTangemPayShownInDetailsAfterBannerClosedTest() {
        val eligibilityStartedState = "Started"
        val channelsBannerState = "Banner"

        setupHooks(
            additionalBeforeSection = { enableTangemPayBannerDismissal() },
            additionalAfterSection = { disableTangemPayBannerDismissal() },
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
            step("Click on 'Later' button on 'Get Tangem Pay' banner") {
                onMainScreen { getTangemPayBannerLaterButton.performClick() }
            }
            step("Assert 'Get Tangem Pay' banner is not displayed on Main") {
                flakySafely { onMainScreen { getTangemPayBanner.assertDoesNotExist() } }
            }
            step("Click on 'More' button") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Assert 'Get Tangem Pay' row is displayed") {
                flakySafely { onDetailsScreen { getTangemPayRow.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("9639")
    @DisplayName("Tangem Pay: Main opens without the Get Tangem Pay banner when eligibility channels fail")
    @Test
    fun getTangemPayBannerNotShownWhenChannelsRequestFailsTest() {
        val eligibilityStartedState = "Started"
        val channelsErrorState = "Error"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityStartedState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsErrorState'") {
                assertTrue(
                    "WireMock should switch '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsErrorState'",
                    setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsErrorState),
                )
            }
            val channelsRequestsBefore = getTangemPayEligibilityChannelsRequestCount()
            step("Open 'Main' screen with existing hot wallet") {
                openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12, accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert eligibility channels are requested") {
                assertTangemPayEligibilityChannelsRequested(channelsRequestsBefore)
            }
            step("Wait for Tangem Pay status 'Not available'") {
                awaitTangemPayStatusOfSelectedWallet(
                    userWalletsListRepository = userWalletsListRepository,
                    paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                    paymentAccountStatusSupplier = paymentAccountStatusSupplier,
                    expectedStatus = PaymentAccountStatusValue.Empty::class,
                )
            }
            step("Assert 'Get Tangem Pay' banner is not displayed") {
                onMainScreen { getTangemPayBanner.assertDoesNotExist() }
            }
        }
    }

    @AllureId("9672")
    @DisplayName("Tangem Pay: Get Tangem Pay banner opens the Tangem Pay onboarding")
    @Test
    fun getTangemPayBannerOpensOnboardingTest() {
        val eligibilityStartedState = "Started"
        val channelsBannerState = "Banner"

        setupHooks().run {
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
            step("Click on 'Learn more' button on 'Get Tangem Pay' banner") {
                onMainScreen { getTangemPayBannerLearnMoreButton.performClick() }
            }
            step("Assert Tangem Pay onboarding screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayOnboardingScreen { title.assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("9674")
    @DisplayName("Tangem Pay: wallet without Tangem Pay shows no banner and offers Get Tangem Pay in Details")
    @Test
    fun walletWithoutTangemPayShowsGetTangemPayOnlyInDetailsTest() {
        val hotWalletCustomerOnlyState = "HotWalletCustomerOnly"
        val channelsBannerState = "Banner"
        val cardWalletName = "Wallet 2"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$hotWalletCustomerOnlyState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, hotWalletCustomerOnlyState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Open 'Main' screen with the Tangem Pay tile of the hot wallet") {
                openMainScreenWithTangemPayTile()
            }
            step("Add card wallet without Tangem Pay") { addNewCardWallet(WalletMockContent) }
            step("Assert '$cardWalletName' wallet is displayed") {
                flakySafely { onMainScreen { walletNameText(cardWalletName).assertIsDisplayed() } }
            }
            step("Wait for Tangem Pay status 'Not available'") {
                awaitTangemPayStatusOfSelectedWallet(
                    userWalletsListRepository = userWalletsListRepository,
                    paymentAccountStatusFetcher = paymentAccountStatusFetcher,
                    paymentAccountStatusSupplier = paymentAccountStatusSupplier,
                    expectedStatus = PaymentAccountStatusValue.Empty::class,
                )
            }
            step("Assert 'Get Tangem Pay' banner is not displayed") {
                onMainScreen { getTangemPayBanner.assertDoesNotExist() }
            }
            step("Click on 'More' button") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Assert 'Get Tangem Pay' row is displayed") {
                flakySafely { onDetailsScreen { getTangemPayRow.assertIsDisplayed() } }
            }
        }
    }
}