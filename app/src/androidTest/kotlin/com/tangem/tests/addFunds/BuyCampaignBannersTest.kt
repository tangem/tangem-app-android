package com.tangem.tests.addFunds

import androidx.compose.ui.test.performTextReplacement
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openAppByDeepLink
import com.tangem.scenarios.openBuyScreenFromTokenDetails
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openProviderPageByBuyButton
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onBuyTokenDetailsScreen
import com.tangem.screens.onBuyTokenFiatListBottomSheet
import com.tangem.screens.onConciergeDemandSurveyScreen
import com.tangem.screens.onOnrampSuccessScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class BuyCampaignBannersTest : BaseTestCase() {

    private val campaignsScenarioName = "marketing_campaigns_onramp"
    private val campaignsState = "OtcAndWaitlist"
    private val quoteScenarioName = "onramp_quote"
    private val slowQuotesState = "SlowQuotes"
    private val fiatRateScenarioName = "onramp_fiat_usd_rate"
    private val fiatRateState = "AudAboveUsd"

    private val tokenTitle = "Polygon"
    private val otcBannerTitle = "Buy with BTC Direct"
    private val waitlistBannerTitle = "From bank to stablecoins 1:1"
    private val waitlistBannerButton = "Join waitlist"

    private val thresholdAmount = "100"
    private val belowThresholdAmount = "99"
    private val withinProviderLimitAmount = "50"
    private val aboveProviderLimitAmount = "20000"
    private val audAmountWorthAboveThreshold = "80"
    private val audCurrency = "AUD"

    @AllureId("13902")
    @DisplayName("Buy. Campaign banners. Display by amount")
    @Test
    fun campaignBannersDisplayByAmountTest() {
        setupHooks().run {
            step("Set WireMock scenario '$campaignsScenarioName' to '$campaignsState'") {
                setWireMockScenarioState(campaignsScenarioName, campaignsState)
            }
            step("Set WireMock scenario '$fiatRateScenarioName' to '$fiatRateState'") {
                setWireMockScenarioState(fiatRateScenarioName, fiatRateState)
            }
            step("Set WireMock scenario '$quoteScenarioName' to '$slowQuotesState'") {
                setWireMockScenarioState(quoteScenarioName, slowQuotesState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Buy' screen from token details and write amount '$belowThresholdAmount'") {
                openBuyScreenFromTokenDetails(tokenTitle = tokenTitle, fiatAmount = belowThresholdAmount)
            }
            step("Assert 'OTC partner' banner is not displayed") {
                onBuyTokenDetailsScreen { marketingBannerText(otcBannerTitle).assertDoesNotExist() }
            }
            step("Assert 'Waitlist' banner is not displayed") {
                onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerTitle).assertDoesNotExist() }
            }
            step("Write amount equal to the threshold '$thresholdAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextReplacement(thresholdAmount) }
            }
            step("Assert offers are still loading") {
                onBuyTokenDetailsScreen { offerTokenAmount.assertDoesNotExist() }
            }
            step("Assert 'OTC partner' banner is displayed while offers are loading") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onBuyTokenDetailsScreen { marketingBannerText(otcBannerTitle).assertIsDisplayed() }
                }
            }
            step("Assert 'Waitlist' banner is displayed while offers are loading") {
                onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerTitle).assertIsDisplayed() }
            }
            step("Assert offer is displayed after loading") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onBuyTokenDetailsScreen { offerTokenAmount.assertIsDisplayed() } }
            }
            step("Assert 'OTC partner' banner stays after loading") {
                onBuyTokenDetailsScreen { marketingBannerText(otcBannerTitle).assertIsDisplayed() }
            }
            step("Assert 'Waitlist' banner stays after loading") {
                onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerTitle).assertIsDisplayed() }
            }
            step("Write amount below the threshold '$belowThresholdAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextReplacement(belowThresholdAmount) }
            }
            step("Assert 'OTC partner' banner is hidden") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onBuyTokenDetailsScreen { marketingBannerText(otcBannerTitle).assertDoesNotExist() }
                }
            }
            step("Assert 'Waitlist' banner is hidden") {
                onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerTitle).assertDoesNotExist() }
            }
            step("Select currency '$audCurrency'") {
                onBuyTokenDetailsScreen { expandFiatListButton.clickWithAssertion() }
                onBuyTokenFiatListBottomSheet { fiatListItemWithTitle(audCurrency).clickWithAssertion() }
            }
            step("Write amount '$audAmountWorthAboveThreshold' whose USD equivalent is not below the threshold") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextReplacement(audAmountWorthAboveThreshold) }
            }
            step("Assert 'OTC partner' banner is displayed again") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onBuyTokenDetailsScreen { marketingBannerText(otcBannerTitle).assertIsDisplayed() }
                }
            }
            step("Assert 'Waitlist' banner is displayed again") {
                onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13903")
    @DisplayName("Buy. Banners and offers. Limits independence")
    @Test
    fun campaignThresholdAndProviderLimitAreIndependentTest() {
        setupHooks().run {
            step("Set WireMock scenario '$campaignsScenarioName' to '$campaignsState'") {
                setWireMockScenarioState(campaignsScenarioName, campaignsState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Buy' screen from token details and write amount within provider limit and below threshold") {
                openBuyScreenFromTokenDetails(tokenTitle = tokenTitle, fiatAmount = withinProviderLimitAmount)
            }
            step("Assert provider offer is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onBuyTokenDetailsScreen { offerTokenAmount.assertIsDisplayed() } }
            }
            step("Assert 'OTC partner' banner is not displayed") {
                onBuyTokenDetailsScreen { marketingBannerText(otcBannerTitle).assertDoesNotExist() }
            }
            step("Assert 'Waitlist' banner is not displayed") {
                onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerTitle).assertDoesNotExist() }
            }
            step("Write amount not below threshold and above provider limit") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextReplacement(aboveProviderLimitAmount) }
            }
            step("Assert maximum purchase amount restriction is displayed under the amount") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onBuyTokenDetailsScreen { maxAmountRestriction.assertIsDisplayed() } }
            }
            step("Assert there are no provider offers") {
                onBuyTokenDetailsScreen { offerTokenAmount.assertDoesNotExist() }
            }
            step("Assert 'OTC partner' banner is displayed") {
                onBuyTokenDetailsScreen { marketingBannerText(otcBannerTitle).assertIsDisplayed() }
            }
            step("Assert 'Waitlist' banner is displayed") {
                onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13907")
    @DisplayName("Buy. Waitlist banner. Opening the survey without resetting Buy")
    @Test
    fun waitlistSurveyOpensWithoutResettingBuyTest() {
        val onrampTransactionId = "mock-onramp-tx-001"
        val providerPageUrl = "https://example.com/onramp-widget"

        setupHooks().run {
            step("Set WireMock scenario '$campaignsScenarioName' to '$campaignsState'") {
                setWireMockScenarioState(campaignsScenarioName, campaignsState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Buy' screen from token details and write amount '$thresholdAmount'") {
                openBuyScreenFromTokenDetails(tokenTitle = tokenTitle, fiatAmount = thresholdAmount)
            }
            step("Assert 'Waitlist' banner is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerTitle).assertIsDisplayed() }
                }
            }
            step("Click on the 'Waitlist' banner button") {
                onBuyTokenDetailsScreen { marketingBannerText(waitlistBannerButton).clickWithAssertion() }
            }
            step("Assert survey bottom sheet is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onConciergeDemandSurveyScreen { navigationTitle.assertIsDisplayed() }
                }
            }
            step("Close the survey bottom sheet") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    runCatching { onConciergeDemandSurveyScreen { navigationTitle.assertDoesNotExist() } }
                        .onFailure { device.uiDevice.pressBack() }
                        .getOrThrow()
                }
            }
            step("Assert survey bottom sheet is closed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onConciergeDemandSurveyScreen { navigationTitle.assertDoesNotExist() }
                }
            }
            step("Assert 'Buy' screen is opened with the same amount") {
                onBuyTokenDetailsScreen { fiatAmountTextField.assertTextContains(thresholdAmount) }
            }
            step("Assert offer is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onBuyTokenDetailsScreen { offerTokenAmount.assertIsDisplayed() } }
            }
            step("Open the provider page by 'Buy' button") {
                openProviderPageByBuyButton(providerPageUrl)
            }
            step("Return from the provider by the deeplink with 'tx_id' and 'result'") {
                openAppByDeepLink("tangem://onramp?tx_id=$onrampTransactionId&result=success")
            }
            step("Assert 'In progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onOnrampSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Assert survey bottom sheet is not opened") {
                onConciergeDemandSurveyScreen { navigationTitle.assertDoesNotExist() }
            }
        }
    }
}