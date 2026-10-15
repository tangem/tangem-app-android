package com.tangem.tests.main

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.setRateAppReadyToShow
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openTokenDetailsFromTokenList
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.tap.domain.sdk.mocks.content.V3LowSignaturesMockContent
import com.tangem.tap.domain.sdk.mocks.content.WalletWithBackupMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class WarningsTest : BaseTestCase() {

    @AllureId("183")
    @DisplayName("Warnings: no account warning on token details")
    @Test
    fun checkNoAccountWarningOnTokenDetailsTest() {
        val tokenName = "XRP Ledger"
        val currencySymbol = "XRP"
        val reserveAmount = "1.00"
        val userTokensState = "XRP"
        val rippleAccountInfoScenario = "ripple_account_info"
        val rippleAccountLinesScenario = "ripple_account_lines"
        val accountNotFoundState = "AccountNotFound"

        setupHooks().run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensState)
            }
            step("Set WireMock scenario: '$rippleAccountInfoScenario' to state: '$accountNotFoundState'") {
                setWireMockScenarioState(rippleAccountInfoScenario, accountNotFoundState)
            }
            step("Set WireMock scenario: '$rippleAccountLinesScenario' to state: '$accountNotFoundState'") {
                setWireMockScenarioState(rippleAccountLinesScenario, accountNotFoundState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Token Details' screen for '$tokenName'") {
                openTokenDetailsFromTokenList(tokenName)
            }
            step("Assert 'Top up wallet' notification title is displayed") {
                flakySafely { onTokenDetailsScreen { topUpYourWalletNotificationTitle.assertIsDisplayed() } }
            }
            step("Assert 'Top up wallet' notification message is displayed") {
                onTokenDetailsScreen {
                    topUpYourWalletNotificationMessage(
                        networkName = tokenName,
                        amount = reserveAmount,
                        currencySymbol = currencySymbol,
                    ).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("184")
    @DisplayName("Warnings: missing address warning")
    @Test
    fun checkUnavailableNetworksWarningTest() {
        val scenarioState = "MissingDerivation"

        setupHooks().run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, scenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            // Synchronising derives the addresses and dismisses the very notification under test.
            step("Assert 'Synchronize addresses' button is displayed") {
                flakySafely { onMainScreen { synchronizeAddressesButton.assertIsDisplayed() } }
            }
            step("Assert 'Missing addresses' notification title is displayed") {
                onMainScreen { missingAddressNotificationTitle.assertIsDisplayed() }
            }
            step("Assert 'Missing addresses' notification message is displayed") {
                onMainScreen { missingAddressNotificationMessage.assertIsDisplayed() }
            }
        }
    }

    @AllureId("12732")
    @DisplayName("Warnings: low signatures warning for v3 card")
    @Test
    fun checkLowSignaturesWarningTest() {
        val remainingSignatures = V3LowSignaturesMockContent.REMAINING_SIGNATURES

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = V3LowSignaturesMockContent)
            }
            step("Assert 'Low signature count' notification title is displayed") {
                flakySafely { onMainScreen { lowSignaturesNotificationTitle.assertIsDisplayed() } }
            }
            step("Assert 'Low signature count' notification message is displayed") {
                onMainScreen { lowSignaturesNotificationMessage(remainingSignatures).assertIsDisplayed() }
            }
        }
    }

    @AllureId("12735")
    @DisplayName("Warnings: rate app banner is displayed")
    @Test
    fun checkRateAppBannerTest() {
        setupHooks(
            additionalPreferencesSection = { preferences -> preferences.setRateAppReadyToShow() },
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = WalletWithBackupMockContent)
            }
            step("Assert 'Rate app' notification title is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onMainScreen { rateAppNotificationTitle.assertIsDisplayed() } }
            }
            step("Assert 'Rate app' notification message is displayed") {
                onMainScreen { rateAppNotificationMessage.assertIsDisplayed() }
            }
            step("Assert 'Could be better' button is displayed") {
                onMainScreen { rateAppCouldBeBetterButton.assertIsDisplayed() }
            }
            step("Assert 'Like it' button is displayed") {
                onMainScreen { rateAppLikeItButton.assertIsDisplayed() }
            }
        }
    }
}