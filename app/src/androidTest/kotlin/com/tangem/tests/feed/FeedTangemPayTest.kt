package com.tangem.tests.feed

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_KYC_STATUS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.scenarios.addNewCardWallet
import com.tangem.scenarios.awaitTangemPayStatusOfSelectedWallet
import com.tangem.scenarios.expandFeed
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.scenarios.openMainScreenWithTangemPayTile
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMarketsScreen
import com.tangem.screens.tangempay.onTangemPayKycScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayOnboardingScreen
import com.tangem.tap.domain.sdk.mocks.content.Wallet2WithDerivationsMockContent
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR

@HiltAndroidTest
class FeedTangemPayTest : BaseTestCase() {

    @Inject
    lateinit var userWalletsListRepository: UserWalletsListRepository

    @Inject
    lateinit var paymentAccountStatusSupplier: PaymentAccountStatusSupplier

    @Test
    @AllureId("11661")
    @DisplayName("Feed: Tangem Pay. Banner is displayed for a single wallet without a Pay account")
    fun tangemPayBannerDisplayedForSingleWalletWithoutPayAccountTest() {
        val channelsBannerState = "Banner"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Tangem Pay' banner is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onMarketsScreen { tangemPayBanner.assertIsDisplayed() } }
            }
            step("Assert 'Tangem Pay' banner title is displayed") {
                onMarketsScreen { tangemPayBannerTitle.assertIsDisplayed() }
            }
            step("Assert 'Tangem Pay' banner subtitle is displayed") {
                onMarketsScreen { tangemPayBannerDescription.assertIsDisplayed() }
            }
        }
    }

    @Test
    @AllureId("11662")
    @DisplayName("Feed: Tangem Pay. No banner for a single wallet with a Pay account")
    fun tangemPayBannerHiddenForSingleWalletWithPayAccountTest() {
        val paeraCustomerState = "PaeraCustomer"
        val channelsBannerState = "Banner"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$paeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, paeraCustomerState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Open 'Main' screen with Tangem Pay tile") {
                openMainScreenWithTangemPayTile()
            }
            step("Assert Tangem Pay balance is displayed on 'Main' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayMainScreen { tileBalance.assertIsDisplayed() } }
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Add account' tile is displayed") {
                flakySafely { onMarketsScreen { addAccountBanner.assertIsDisplayed() } }
            }
            step("Assert 'Tangem Pay' banner is not displayed") {
                onMarketsScreen { tangemPayBanner.assertDoesNotExist() }
            }
        }
    }

    @Test
    @AllureId("11663")
    @DisplayName("Feed: Tangem Pay. Banner is displayed for several wallets without a Pay account")
    fun tangemPayBannerDisplayedForSeveralWalletsWithoutPayAccountTest() {
        val channelsBannerState = "Banner"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Add second card wallet") {
                addNewCardWallet(Wallet2WithDerivationsMockContent)
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Tangem Pay' banner is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onMarketsScreen { tangemPayBanner.assertIsDisplayed() } }
            }
            step("Assert 'Tangem Pay' banner title is displayed") {
                onMarketsScreen { tangemPayBannerTitle.assertIsDisplayed() }
            }
            step("Assert 'Tangem Pay' banner subtitle is displayed") {
                onMarketsScreen { tangemPayBannerDescription.assertIsDisplayed() }
            }
        }
    }

    @Test
    @AllureId("11664")
    @DisplayName("Feed: Tangem Pay. No banner for several wallets when one has a Pay account")
    fun tangemPayBannerHiddenForSeveralWalletsWhenOneHasPayAccountTest() {
        val hotWalletCustomerOnlyState = "HotWalletCustomerOnly"
        val channelsBannerState = "Banner"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$hotWalletCustomerOnlyState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, hotWalletCustomerOnlyState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Open 'Main' screen with Tangem Pay tile") {
                openMainScreenWithTangemPayTile()
            }
            step("Assert Tangem Pay balance is displayed on 'Main' screen") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayMainScreen { tileBalance.assertIsDisplayed() } }
            }
            step("Add second card wallet") {
                addNewCardWallet(WalletMockContent)
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Add account' tile is displayed") {
                flakySafely { onMarketsScreen { addAccountBanner.assertIsDisplayed() } }
            }
            step("Assert 'Tangem Pay' banner is not displayed") {
                onMarketsScreen { tangemPayBanner.assertDoesNotExist() }
            }
        }
    }

    @Test
    @AllureId("11665")
    @DisplayName("Feed: Tangem Pay. Click on banner opens Tangem Pay onboarding")
    fun tangemPayBannerClickOpensOnboardingTest() {
        val channelsBannerState = "Banner"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBannerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBannerState)
            }
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Tangem Pay' banner is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onMarketsScreen { tangemPayBanner.assertIsDisplayed() } }
            }
            step("Click on 'Tangem Pay' banner") {
                onMarketsScreen { tangemPayBanner.performClick() }
            }
            step("Assert Tangem Pay onboarding screen is displayed") {
                flakySafely {
                    onTangemPayOnboardingScreen {
                        title.assertIsDisplayed()
                        getCardButton.assertIsDisplayed()
                    }
                }
            }
        }
    }

    @Test
    @AllureId("12561")
    @DisplayName("Feed: Tangem Pay. Click on banner opens KYC when KYC is not completed")
    fun tangemPayBannerClickOpensKycWhenKycNotCompletedTest() {
        val paeraCustomerState = "PaeraCustomer"
        val kycInProgressState = "InProgress"
        val kycInProgressText = getResourceString(CoreResR.string.tangempay_kyc_in_progress)
        val kycEndpoint = "/bff-v2/v1/customer/kyc"

        setupHooks().run {
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
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Tangem Pay' banner is displayed") {
                flakySafely { onMarketsScreen { tangemPayBanner.assertIsDisplayed() } }
            }
            val kycRequestsBefore = getWireMockRequestCount("GET", kycEndpoint)
            step("Click on 'Tangem Pay' banner") {
                onMarketsScreen { tangemPayBanner.performClick() }
            }
            step("Assert KYC screen is displayed") {
                flakySafely { onTangemPayKycScreen { screenContainer.assertIsDisplayed() } }
            }
            step("Assert KYC screen requested '$kycEndpoint'") {
                flakySafely {
                    assertTrue(
                        "KYC screen should request $kycEndpoint",
                        getWireMockRequestCount("GET", kycEndpoint) > kycRequestsBefore,
                    )
                }
            }
        }
    }

    @Test
    @AllureId("12597")
    @DisplayName("Feed: Tangem Pay. No banner when Tangem Pay is not available for the user")
    fun tangemPayBannerHiddenWhenTangemPayNotAvailableTest() {
        val channelsStartedState = "Started"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsStartedState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsStartedState)
            }
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Wait for Tangem Pay status 'Not available'") {
                awaitTangemPayStatusOfSelectedWallet(
                    userWalletsListRepository = userWalletsListRepository,
                    paymentAccountStatusSupplier = paymentAccountStatusSupplier,
                    expectedStatus = PaymentAccountStatusValue.Empty::class,
                )
            }
            step("Assert 'Get Tangem Pay' banner is not displayed on 'Main' screen") {
                onMainScreen { getTangemPayBanner.assertDoesNotExist() }
            }
            step("Expand feed") {
                expandFeed()
            }
            step("Assert 'Add account' tile is displayed") {
                flakySafely { onMarketsScreen { addAccountBanner.assertIsDisplayed() } }
            }
            step("Assert 'Tangem Pay' banner is not displayed") {
                onMarketsScreen { tangemPayBanner.assertDoesNotExist() }
            }
        }
    }
}