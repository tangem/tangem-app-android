package com.tangem.tests.actionButtons

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.BITCOIN_ADDRESS
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertClipboardTextEquals
import com.tangem.common.utils.clearClipboard
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MainScreenActionButtonsTest : BaseTestCase() {

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
        val tokenSymbol = "BTC"

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
            step("Assert token amount field is displayed") {
                onBuyTokenDetailsScreen { tokenAmountField.assertTextContains(tokenSymbol, substring = true) }
            }
            step("Assert 'Continue' button") {
                onBuyTokenDetailsScreen { continueButton.assertIsDisplayed() }
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
            step("Assert 'Token receive warning' bottom sheet is displayed") {
                waitForIdle()
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTokenReceiveWarningBottomSheet {
                        bottomSheet.assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Got it' button") {
                onTokenReceiveWarningBottomSheet { gotItButton.performClick() }
            }
            step("Click on 'Show QR code' button") {
                onReceiveAssetsBottomSheet { showQrCodeButton.clickWithAssertion() }
            }
            step("Assert bottom sheet with QR code title is displayed") {
                onTokenReceiveQrCodeBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert QR code is displayed") {
                onTokenReceiveQrCodeBottomSheet { qrCode.assertIsDisplayed() }
            }
            step("Assert address title is displayed") {
                onTokenReceiveQrCodeBottomSheet { addressTitle.assertIsDisplayed() }
            }
            step("Assert address is displayed") {
                onTokenReceiveQrCodeBottomSheet { address.assertIsDisplayed() }
            }
            step("Assert 'Copy' button is displayed") {
                onTokenReceiveQrCodeBottomSheet { copyButton.assertIsDisplayed() }
            }
            step("Assert 'Share' button is displayed") {
                onTokenReceiveQrCodeBottomSheet { shareButton.assertIsDisplayed() }
            }
        }
    }

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
            step("Assert 'Receive' button is displayed") {
                onTokenActionsBottomSheet { sellButton.assertIsDisplayed() }
            }
            step("Click on 'Receive' button") {
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
}