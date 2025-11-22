package com.tangem.tests.actionButtons

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.annotations.ApiEnv
import com.tangem.common.annotations.ApiEnvConfig
import com.tangem.common.constants.TestConstants.BITCOIN_ADDRESS
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.*
import com.tangem.common.utils.*
import com.tangem.datasource.api.common.config.ApiConfig
import com.tangem.datasource.api.common.config.ApiEnvironment
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.MockContent
import com.tangem.tap.domain.sdk.mocks.content.TwinsMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MainScreenActionButtonsTest : BaseTestCase() {

    @ApiEnv(ApiEnvConfig(ApiConfig.ID.MoonPay, ApiEnvironment.PROD))
    @AllureId("79")
    @DisplayName("Action buttons (long tap): validate UI")
    @Test
    fun actionButtonsValidateLongTapUiTest() {
        val tokenTitle = "Ethereum"
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Assert 'Analytics' button is displayed") {
                onTokenActionsBottomSheet { analyticsButton.assertIsDisplayed() }
            }
            step("Assert 'Copy address' button is displayed") {
                onTokenActionsBottomSheet { copyAddressButton.assertIsDisplayed() }
            }
            step("Assert 'Receive' button is displayed") {
                onTokenActionsBottomSheet { receiveButton.assertIsDisplayed() }
            }
            step("Assert 'Send' button is displayed") {
                onTokenActionsBottomSheet { sendButton.assertIsDisplayed() }
            }
            step("Assert 'Swap' button is displayed") {
                onTokenActionsBottomSheet { swapButton.assertIsDisplayed() }
            }
            step("Assert 'Buy' button is displayed") {
                onTokenActionsBottomSheet { buyButton.assertIsDisplayed() }
            }
            step("Assert 'Sell' button is displayed") {
                onTokenActionsBottomSheet { sellButton.assertIsDisplayed() }
            }
            step("Assert 'Hide token' button is displayed") {
                onTokenActionsBottomSheet { hideTokenButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("84")
    @DisplayName("Action buttons (long tap): check 'Copy address' button")
    @Test
    fun clickOnCopyAddressButtonTest() {
        val tokenTitle = "Bitcoin"
        val bitcoinAddress = BITCOIN_ADDRESS

        setupHooks(
            additionalBeforeSection = {
                clearClipboard()
            },
            additionalAfterSection = {
                clearClipboard()
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Assert 'Copy address' button is displayed") {
                onTokenActionsBottomSheet { copyAddressButton.assertIsDisplayed() }
            }
            step("Click on 'Copy address' button") {
                onTokenActionsBottomSheet { copyAddressButton.performClick() }
            }
            step("Assert snack bar message is displayed") {
                onMainScreen { snackbarCopiedAddressMessage.assertIsDisplayed() }
            }
            step("Check clipboard has '$tokenTitle' address '$bitcoinAddress'") {
                waitForIdle()
                assertClipboardTextEquals(expected = bitcoinAddress)
            }
        }
    }

    @AllureId("82")
    @DisplayName("Action buttons (long tap): check 'Buy' button")
    @Test
    fun clickOnBuyButtonTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Assert 'Buy' button is displayed") {
                onTokenActionsBottomSheet { buyButton.assertIsDisplayed() }
            }
            step("Click on 'Buy' button") {
                onTokenActionsBottomSheet { buyButton.performClick() }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                waitForIdle()
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Assert top app bar title contains '$tokenTitle'") {
                onBuyTokenDetailsScreen { topBarTitle.assertTextContains("Buy $tokenTitle") }
            }
            step("Assert fiat currency text field is displayed") {
                onBuyTokenDetailsScreen { fiatAmountTextField.assertIsDisplayed() }
            }
            step("Assert fiat currency icon is displayed") {
                onBuyTokenDetailsScreen { fiatCurrencyIcon.assertIsDisplayed() }
            }
        }
    }

    @AllureId("87")
    @DisplayName("Action buttons (long tap): check 'Swap' button")
    @Test
    fun clickOnSwapButtonTest() {
        val tokenTitle = "Ethereum"
        val tokenSymbol = "ETH"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Assert 'Swap' button is displayed") {
                onTokenActionsBottomSheet { swapButton.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onTokenActionsBottomSheet { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert token symbol: '$tokenSymbol' is displayed") {
                onSwapTokenScreen { tokenSymbol(tokenSymbol).assertIsDisplayed() }
            }
        }
    }

    @AllureId("83")
    @DisplayName("Action buttons (long tap): check 'Send' button")
    @Test
    fun clickOnSendButtonTest() {
        val tokenTitle = "Ethereum"
        val tokenSymbol = "ETH"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Assert 'Send' button is displayed") {
                onTokenActionsBottomSheet { sendButton.assertIsDisplayed() }
            }
            step("Click on 'Send' button") {
                onTokenActionsBottomSheet { sendButton.performClick() }
            }
            step("Assert amount input text field contains token symbol: '$tokenSymbol'") {
                onSendScreen {
                    amountInputTextField.assertTextContains(value = tokenSymbol, substring = true)
                }
            }
        }
    }

    @AllureId("86")
    @DisplayName("Action buttons (long tap): check 'Receive' button")
    @Test
    fun clickOnReceiveButtonTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Assert 'Receive' button is displayed") {
                onTokenActionsBottomSheet { receiveButton.assertIsDisplayed() }
            }
            step("Click on 'Receive' button") {
                onTokenActionsBottomSheet { receiveButton.performClick() }
            }
            step("Go to QR code bottom sheet") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    goToQrCodeBottomSheet()
                }
            }
            step("Check QR code bottom sheet") {
                checkQrCodeBottomSheetScenario()
            }
        }
    }

    @ApiEnv(ApiEnvConfig(ApiConfig.ID.MoonPay, ApiEnvironment.PROD))
    @AllureId("85")
    @DisplayName("Action buttons (long tap): check 'Sell' button")
    @Test
    fun clickOnSellButtonTest() {
        val tokenTitle = "Ethereum"
        val url = "sell.moonpay.com"
        val useWithoutAccount = "Use without an account"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Assert 'Sell' button is displayed") {
                onTokenActionsBottomSheet { sellButton.assertIsDisplayed() }
            }
            step("Click on 'Sell' button") {
                onTokenActionsBottomSheet { sellButton.performClick() }
            }
            step("Assert Chrome Browser is opened") {
                ThirdPartyAppPageObject { assertChromeIsOpened() }
            }
            if (ThirdPartyAppPageObject.isElementWithTextExists(useWithoutAccount)) {
                step("Click on '$useWithoutAccount' button on Chrome browser") {
                    ThirdPartyAppPageObject { clickOnElementWithText(useWithoutAccount) }
                }
            }
            step("Assert url contains: '$url'") {
                ThirdPartyAppPageObject { assertUrlContains(url) }
            }
        }
    }

    @AllureId("77")
    @DisplayName("Action buttons (long tap): assert 'Sell' button is not displayed if token doesn't support it")
    @Test
    fun assertSellButtonIsNotDisplayedTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Long click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen {
                    tokenWithTitleAndAddress(tokenTitle).performTouchInput {
                        longClick(
                            position = center,
                            durationMillis = 1000L
                        )
                    }
                }
            }
            step("Assert 'Sell' button is not displayed") {
                onTokenActionsBottomSheet { sellButton.assertIsNotDisplayed() }
            }
        }
    }

    @AllureId("895")
    @DisplayName("Action buttons: check blockchain information by click on 'Buy' button")
    @Test
    fun checkClickOnBuyButtonOnMainTest() {
        val cardType: MockContent = TwinsMockContent
        val cardName = "Twin"
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen' on '$cardName' card") {
                openMainScreen(mockContent = cardType, isTwinsCard = true)
            }
            step("Assert 'Buy' button is displayed") {
                onMainScreen { buyButton.assertIsDisplayed() }
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.performClick() }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                waitForIdle()
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Assert top app bar title contains '$tokenTitle'") {
                onBuyTokenDetailsScreen { topBarTitle.assertTextContains("Buy $tokenTitle") }
            }
            step("Assert fiat currency text field is displayed") {
                onBuyTokenDetailsScreen { fiatAmountTextField.assertIsDisplayed() }
            }
            step("Assert fiat currency icon is displayed") {
                onBuyTokenDetailsScreen { fiatCurrencyIcon.assertIsDisplayed() }
            }
        }
    }

    @ApiEnv(ApiEnvConfig(ApiConfig.ID.MoonPay, ApiEnvironment.PROD))
    @AllureId("4395")
    @DisplayName("Action buttons (main screen): click on buttons with success response")
    @Test
    fun clickOnActionButtonsWithSuccessResponseTest() {
        val tokenTitle = "Ethereum"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert 'Buy' button is displayed") {
                onMainScreen { buyButton.assertIsDisplayed() }
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.performClick() }
            }
            step("Assert 'Buy' screen title is displayed") {
                onBuyTokenScreen { topAppBarTitle.assertIsDisplayed() }
            }
            step("Assert token with title: '$tokenTitle' is displayed") {
                onBuyTokenScreen { tokenWithTitleAndFiatAmount(tokenTitle).assertIsDisplayed() }
            }
            step("Press 'Back' button") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Swap' button is displayed") {
                onMainScreen { swapButton.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onMainScreen { swapButton.performClick() }
            }
            step("Click on close button on stories screen") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Swap' token screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Press 'Back' button") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Sell' button is displayed") {
                onMainScreen { sellButton.assertIsDisplayed() }
            }
            step("Click on 'Sell' button") {
                onMainScreen { sellButton.performClick() }
            }
            step("Assert 'Sell' token screen title is displayed") {
                onSellScreen { title.assertIsDisplayed() }
            }
        }
    }

    @AllureId("4396")
    @DisplayName("Action buttons (main screen): click on buttons without data")
    @Test
    fun clickOnActionButtonsWithoutDataTest() {
        setupHooks(
            additionalAfterSection = {
                enableWiFi()
                enableMobileData()
            }
        ).run {
            step("Turn off internet") {
                disableWiFi()
                disableMobileData()
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Assert 'Buy' button is displayed") {
                onMainScreen { buyButton.assertIsDisplayed() }
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.performClick() }
            }
            step("Check 'Action is unavailable' dialog") {
                checkActionIsUnavailableDialog()
            }
            step("Click on 'Ok' button") {
                onDialog { okButton.performClick() }
            }
            step("Assert 'Swap' button is displayed") {
                onMainScreen { swapButton.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onMainScreen { swapButton.performClick() }
            }
            step("Check 'Action is unavailable' dialog") {
                checkActionIsUnavailableDialog()
            }
            step("Click on 'Ok' button") {
                onDialog { okButton.performClick() }
            }
            step("Assert 'Sell' button is displayed") {
                onMainScreen { sellButton.assertIsDisplayed() }
            }
            step("Click on 'Sell' button") {
                onMainScreen { sellButton.performClick() }
            }
            step("Check 'Action is unavailable' dialog") {
                checkActionIsUnavailableDialog()
            }
            step("Click on 'Ok' button") {
                onDialog { okButton.performClick() }
            }
        }
    }

    @AllureId("4398")
    @DisplayName("Action buttons (main screen): click on buttons with error response")
    @Test
    fun clickOnActionButtonsWithErrorResponseTest() {
        val scenarioName = "express_api_assets"
        val scenarioState = "Error"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName, scenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Assert 'Buy' button is displayed") {
                onMainScreen { buyButton.assertIsDisplayed() }
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.performClick() }
            }
            step("Check 'Action is unavailable' dialog") {
                checkActionIsUnavailableDialog()
            }
            step("Click on 'Ok' button") {
                onDialog { okButton.performClick() }
            }
            step("Assert 'Swap' button is displayed") {
                onMainScreen { swapButton.assertIsDisplayed() }
            }
            step("Click on 'Swap' button") {
                onMainScreen { swapButton.performClick() }
            }
            step("Check 'Action is unavailable' dialog") {
                checkActionIsUnavailableDialog()
            }
            step("Click on 'Ok' button") {
                onDialog { okButton.performClick() }
            }
            step("Assert 'Sell' button is displayed") {
                onMainScreen { sellButton.assertIsDisplayed() }
            }
            step("Click on 'Sell' button") {
                onMainScreen { sellButton.performClick() }
            }
            step("Check 'Action is unavailable' dialog") {
                checkActionIsUnavailableDialog()
            }
            step("Click on 'Ok' button") {
                onDialog { okButton.performClick() }
            }
        }
    }

    @AllureId("3642")
    @DisplayName("Action buttons (main screen): check buttons state")
    @Test
    fun checkButtonsStateTest() {
        val scenarioName = "user_tokens_api"
        val scenarioState = "EmptyTokensList"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Assert action buttons is not enabled") {
                assertActionButtonsForMultiCurrencyWallet(isEnabled = false)
            }
            step("Reset Wiremock scenario: '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }
            step("Perform pull to refresh") {
                pullToRefresh(steps = 10)
                waitForIdle()
            }
            step("Assert action buttons is enabled") {
                assertActionButtonsForMultiCurrencyWallet(isEnabled = true)
            }
            step("Set WireMock scenario: '$scenarioName' to state: '$scenarioState'") {
                setWireMockScenarioState(scenarioName = scenarioName, state = scenarioState)
            }
            step("Perform pull to refresh") {
                pullToRefresh(steps = 10)
                waitForIdle()
            }
            step("Assert action buttons is not enabled") {
                assertActionButtonsForMultiCurrencyWallet(isEnabled = false)
            }
        }
    }
}