package com.tangem.tests.actionButtons

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.checkQrCodeBottomSheetScenario
import com.tangem.scenarios.goToQrCodeBottomSheet
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSendFromTokenDetails
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onAddFundsBottomSheet
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSendScreen
import com.tangem.screens.onSwapStoriesScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTransferBottomSheet
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TokenDetailsScreenActionButtonsTest : BaseTestCase() {

    @AllureId("594")
    @DisplayName("Action buttons (token details screen): validate UI")
    @Test
    fun actionButtonsValidateUiTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Assert 'Add funds' button is displayed") {
                onTokenDetailsScreen { addFundsButton.assertIsDisplayed() }
            }
            step("Assert 'Swap' button is displayed") {
                onTokenDetailsScreen { swapButton.assertIsDisplayed() }
            }
            step("Assert 'Transfer' button is displayed") {
                onTokenDetailsScreen { transferButton.assertIsDisplayed() }
            }
            step("Click on 'Add funds' button") {
                onTokenDetailsScreen { addFundsButton.clickWithAssertion() }
            }
            step("Assert 'Buy' button in bottom sheet is displayed") {
                onAddFundsBottomSheet { buyButton.assertIsDisplayed() }
            }
            step("Assert 'Swap' button in bottom sheet is displayed") {
                onAddFundsBottomSheet { swapButton.assertIsDisplayed() }
            }
            step("Assert 'Receive' button in bottom sheet is displayed") {
                onAddFundsBottomSheet { receiveButton.assertIsDisplayed() }
            }
            step("Click on 'Close' button in bottom sheet") {
                onAddFundsBottomSheet { closeButton.clickWithAssertion() }
            }
            step("Click on 'Transfer' button") {
                onTokenDetailsScreen { transferButton.clickWithAssertion() }
            }
            step("Assert 'Send' button in bottom sheet is displayed") {
                onTransferBottomSheet { sendButton.assertIsDisplayed() }
            }
            step("Assert 'Swap' button in bottom sheet is displayed") {
                onTransferBottomSheet { swapButton.assertIsDisplayed() }
            }
            step("Assert 'Sell' button in bottom sheet is displayed") {
                onTransferBottomSheet { sellButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("593")
    @DisplayName("Action buttons (token details screen): check buttons state")
    @Test
    fun checkActionButtonsStateTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Assert 'Add funds' button is enabled") {
                onTokenDetailsScreen { addFundsButton.assertIsEnabled() }
            }
            step("Assert 'Swap' button is disabled") {
                onTokenDetailsScreen { swapButton.assertIsNotEnabled() }
            }
            step("Assert 'Transfer' button is enabled") {
                onTokenDetailsScreen { transferButton.assertIsEnabled() }
            }
            step("Click on 'Add funds' button") {
                onTokenDetailsScreen { addFundsButton.clickWithAssertion() }
            }
            step("Assert 'Buy' button in bottom sheet is enabled") {
                onAddFundsBottomSheet { buyButton.assertIsEnabled() }
            }
            step("Assert 'Swap' button in bottom sheet is disabled") {
                onAddFundsBottomSheet { swapButton.assertIsNotEnabled() }
            }
            step("Assert 'Receive' button in bottom sheet is enabled") {
                onAddFundsBottomSheet { receiveButton.assertIsEnabled() }
            }
            step("Click on 'Close' button in bottom sheet") {
                onAddFundsBottomSheet { closeButton.clickWithAssertion() }
            }
            step("Click on 'Transfer' button") {
                onTokenDetailsScreen { transferButton.clickWithAssertion() }
            }
            step("Assert 'Send' button in bottom sheet is enabled") {
                onTransferBottomSheet { sendButton.assertIsEnabled() }
            }
            step("Assert 'Swap' button in bottom sheet is disabled") {
                onTransferBottomSheet { swapButton.assertIsNotEnabled() }
            }
            step("Assert 'Sell' button in bottom sheet is disabled") {
                onTransferBottomSheet { sellButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("4459")
    @DisplayName("Action buttons (token details screen): check 'Swap' button (success)")
    @Test
    fun checkSwapButtonSuccessTest() {
        val tokenTitle = "Ethereum"
        val tokenSymbol = "ETH"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton.performClick() }
            }
            step("Close 'Stories' screen") {
                onSwapStoriesScreen { closeButton.clickWithAssertion() }
            }
            step("Assert 'Swap' screen title is displayed") {
                onSwapTokenScreen { title.assertIsDisplayed() }
            }
            step("Assert token symbol: '$tokenSymbol' is displayed") {
                onSwapTokenScreen { swapTokenSymbol(tokenSymbol).assertIsDisplayed() }
            }
        }
    }

    @AllureId("3590")
    @DisplayName("Action buttons (token details screen): validate UI")
    @Test
    fun checkReceiveButtonTest() {
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Click on 'Add funds' button") {
                onTokenDetailsScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on 'Receive' button in bottom sheet") {
                onAddFundsBottomSheet { receiveButton.clickWithAssertion() }
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

    @AllureId("591")
    @DisplayName("Action buttons (token details screen): send available for funded token, unavailable for empty token")
    @Test
    fun checkSendAvailabilityForFundedAndEmptyTokenTest() {
        val emptyTokenTitle = "Polygon"
        val fundedTokenTitle = "Ethereum"
        val polygonBalanceScenarioName = "polygon_coin_balance"
        val polygonBalanceScenarioState = "ZeroBalance"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(polygonBalanceScenarioName)
            }
        ).run {
            step("Set WireMock scenario: '$polygonBalanceScenarioName' to state: '$polygonBalanceScenarioState'") {
                setWireMockScenarioState(polygonBalanceScenarioName, polygonBalanceScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$emptyTokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(emptyTokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Assert 'Transfer' button is not displayed for the empty token") {
                onTokenDetailsScreen { transferButton.assertIsNotDisplayed() }
            }
            step("Go back to 'Main Screen'") {
                device.uiDevice.pressBack()
            }
            step("Assert 'Main Screen' is displayed") {
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Click on token with name: '$fundedTokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(fundedTokenTitle).clickWithAssertion() }
            }
            step("Assert 'Transfer' button is displayed for the funded token") {
                onTokenDetailsScreen { transferButton.assertIsDisplayed() }
            }
            step("Open the send flow from token details") {
                openSendFromTokenDetails()
            }
            step("Assert 'Send' screen is displayed") {
                onSendScreen { amountInputTextField.assertIsDisplayed() }
            }
        }
    }
}