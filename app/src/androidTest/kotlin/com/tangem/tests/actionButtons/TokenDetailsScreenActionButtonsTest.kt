package com.tangem.tests.actionButtons

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.SwipeDirection
import com.tangem.common.extensions.assertIsDimmed
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.swipeVertical
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.checkQrCodeBottomSheetScenario
import com.tangem.scenarios.goToQrCodeBottomSheet
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
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
            step("Assert 'Receive' button is displayed") {
                onTokenDetailsScreen { receiveButton().assertIsDisplayed() }
            }
            step("Assert 'Buy' button is displayed") {
                onTokenDetailsScreen { buyButton().assertIsDisplayed() }
            }
            step("Assert 'Send' button is displayed") {
                onTokenDetailsScreen { sendButton().assertIsDisplayed() }
            }
            step("Assert 'Swap' button is displayed") {
                onTokenDetailsScreen { swapButton().assertIsDisplayed() }
            }
            step("Assert 'Sell' button is displayed") {
                onTokenDetailsScreen { sellButton().assertIsDisplayed() }
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
            step("Assert 'Receive' button is not dimmed") {
                onTokenDetailsScreen { receiveButton().assertIsDimmed(false) }
            }
            step("Assert 'Buy' button is not dimmed") {
                onTokenDetailsScreen { buyButton().assertIsDimmed(false) }
            }
            step("Assert 'Send' button is not dimmed") {
                onTokenDetailsScreen { sendButton().assertIsDimmed(false) }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton().assertIsDimmed() }
            }
            step("Assert 'Sell' button is dimmed") {
                onTokenDetailsScreen { sellButton().assertIsDimmed() }
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
                onTokenDetailsScreen { swapButton().performClick() }
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

    @AllureId("4460")
    @DisplayName("Action buttons (token details screen): check 'Swap' button (provider error)")
    @Test
    fun checkSwapButtonProviderErrorTest() {
        val tokenTitle = "POL (ex-MATIC)"

        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Swipe up") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton().assertIsDimmed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
            }
            step("Assert swapping $tokenTitle is not supported dialog text is displayed") {
                onSwapIsNotSupportedDialog { text(tokenTitle).assertIsDisplayed() }
            }
            step("Assert 'Ok' button is displayed") {
                onSwapIsNotSupportedDialog { okButton.assertIsDisplayed() }
            }
            step("Click on 'Ok' button") {
                onSwapIsNotSupportedDialog { okButton.performClick() }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton().assertIsDimmed() }
            }
        }
    }

    @AllureId("4461")
    @DisplayName("Action buttons (token details screen): check 'Swap' button (Express error)")
    @Test
    fun checkSwapButtonExpressErrorTest() {
        val tokenTitle = "Polygon"
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
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Swipe up") {
                swipeVertical(SwipeDirection.UP)
            }
            step("Click on token with name: '$tokenTitle'") {
                waitForIdle()
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).performClick() }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton().assertIsDimmed() }
            }
            step("Click on 'Swap' button") {
                onTokenDetailsScreen { swapButton().performClick() }
            }
            step("Assert operation is unavailable dialog text is displayed") {
                onOperationIsUnavailableDialog { text.assertIsDisplayed() }
            }
            step("Assert 'Ok' button is displayed") {
                onOperationIsUnavailableDialog { okButton.assertIsDisplayed() }
            }
            step("Click on 'Ok' button") {
                onOperationIsUnavailableDialog { okButton.performClick() }
            }
            step("Assert 'Swap' button is dimmed") {
                onTokenDetailsScreen { swapButton().assertIsDimmed() }
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
            step("Click on 'Receive' button") {
                onTokenDetailsScreen { receiveButton().performClick() }
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
}