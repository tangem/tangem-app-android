package com.tangem.scenarios

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.HOLD_DURATION_MS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.ui.R
import com.kaspersky.kaspresso.testcases.core.testcontext.TestContext
import com.tangem.screens.*
import com.tangem.tap.domain.sdk.mocks.MockContent
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openSendScreen(
    tokenName: String,
    mockState: String = "",
    mockContent: MockContent? = null,
) {
    val scenarioState = mockState.ifEmpty { tokenName }
    step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
        setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = scenarioState)
    }
    step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$scenarioState'") {
        setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = scenarioState)
    }
    step("Open 'Main Screen'") {
        openMainScreen(mockContent = mockContent)
    }
    step("Synchronize addresses") {
        synchronizeAddresses()
    }
    step("Click on token with name: '$tokenName'") {
        onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
    }
    step("Click on 'Transfer' button") {
        onTokenDetailsScreen { transferButton.clickWithAssertion() }
    }
    step("Click on 'Send' button in bottom sheet") {
        onTransferBottomSheet { sendButton.clickWithAssertion() }
    }
}

fun BaseTestCase.checkNetworkFeeBlock(currentFeeAmount: String, withFeeSelector: Boolean) {
    step("Assert fee selector block is displayed") {
        onSendConfirmScreen { feeSelectorBlock.assertIsDisplayed() }
    }
    step("Assert fee selector icon is displayed") {
        onSendConfirmScreen { feeSelectorIcon.assertIsDisplayed() }
    }
    step("Assert fee selector title is displayed") {
        onSendConfirmScreen { feeSelectorTitle.assertIsDisplayed() }
    }
    step("Assert fee selector tooltip icon is displayed") {
        onSendConfirmScreen { feeSelectorTooltipIcon.assertIsDisplayed() }
    }
    step("Assert fee amount = '$currentFeeAmount'") {
        onSendConfirmScreen { feeAmount.assertTextContains(currentFeeAmount) }
    }
    if (withFeeSelector) {
        step("Assert select fee icon is displayed") {
            onSendConfirmScreen { selectFeeIcon.assertIsDisplayed() }
        }
    } else {
        step("Assert select fee icon is not displayed") {
            onSendConfirmScreen { selectFeeIcon.assertIsNotDisplayed() }
        }
    }
}

fun BaseTestCase.openSendConfirmScreen(
    tokenName: String,
    inputAmount: String,
    recipientAddress: String
) {
    step("Open 'Send Address' screen") {
        openSendAddressScreen(tokenName, inputAmount)
    }
    step("Type recipient address") {
        onSendAddressScreen { addressTextField.performTextReplacement(recipientAddress) }
    }
    step("Click 'Next' button until 'Send Confirm' screen opens") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching { openSendConfirmScreenViaNextButton() }.isSuccess
        }
    }
}

fun BaseTestCase.openSendAddressScreen(
    tokenName: String,
    inputAmount: String
) {
    step("Click on token with name: '$tokenName'") {
        onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
    }
    step("Click on 'Transfer' button") {
        onTokenDetailsScreen { transferButton.clickWithAssertion() }
    }
    step("Click on 'Send' button in bottom sheet") {
        onTransferBottomSheet { sendButton.clickWithAssertion() }
    }
    step("Type '$inputAmount' in input text field") {
        onSendScreen {
            amountInputTextField.performClick()
            amountInputTextField.performTextReplacement(inputAmount)
        }
    }
    step("Click on 'Next' button") {
        onSendScreen { nextButton.clickWithAssertion() }
    }
    step("Assert 'Send Address' container is displayed") {
        onSendAddressScreen { container.assertIsDisplayed() }
    }
    step("Wait for recipient list to load") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching {
                onSendAddressScreen { addressesShimmer.assertIsNotDisplayed() }
            }.isSuccess
        }
    }
}

fun BaseTestCase.checkScanQrScreen(emptyClipboard: Boolean = true) {
    step("Assert 'Back' button is displayed") {
        onScanQrScreen { backTopAppBarButton.assertIsDisplayed() }
    }
    step("Assert 'Flashlight' button is displayed") {
        onScanQrScreen { flashlightButton.assertIsDisplayed() }
    }
    step("Assert 'Gallery' button is displayed") {
        onScanQrScreen { galleryButton.assertIsDisplayed() }
    }
    if (!emptyClipboard) {
        step("Assert 'Paste from clipboard' is displayed") {
            onScanQrScreen { pasteFromClipboardButton.assertIsDisplayed() }
        }
    }
}

fun BaseTestCase.checkDestinationTagBlock(hint: String) {
    step("Assert 'Destination Tag' title is displayed") {
        onSendAddressScreen { destinationTagBlockTitle().assertIsDisplayed() }
    }
    step("Assert 'Destination Tag' text is displayed") {
        onSendAddressScreen { destinationTagBlockText.assertIsDisplayed() }
    }
    step("Assert 'Destination Tag' text field hint contains text '$hint'") {
        onSendAddressScreen { destinationTagTextFieldHint.assertTextContains(hint) }
    }
    step("Assert 'Destination Tag' text field is displayed") {
        onSendAddressScreen { destinationTagTextField.assertIsDisplayed() }
    }
    step("Assert 'Destination Tag' caution is displayed") {
        onSendAddressScreen { destinationTagBlockCaution.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkRecentAddressItem(address: String, description: String?) {
    step("Assert recent address '$address' with description '$description' is displayed") {
        onSendAddressScreen {
            recentAddressItem(recipientAddress = address, description = description).assertIsDisplayed()
        }
    }
}

fun BaseTestCase.checkCustomFeeTooltip(title: String, tooltip: String) {
    step("Click on tooltip icon for '$title'") {
        waitForIdle()
        onSendSelectNetworkFeeBottomSheet { tooltipIcon(title).performClick() }
    }
    step("Check '$title' tooltip text") {
        onSendSelectNetworkFeeBottomSheet { tooltipText(tooltip).assertIsDisplayed() }
    }
    step("Click on tooltip icon again to close tooltip") {
        onSendSelectNetworkFeeBottomSheet { tooltipIcon(title).performClick() }
    }
}

fun BaseTestCase.checkChangesInInputTextField(title: String, newValue: String, addition: String = "") {
    step("Click on '$title' input text field") {
        onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(title).performClick() }
    }
    step("Type '$newValue' in '$title' input text field") {
        onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(title).performTextReplacement(newValue) }
    }
    step("Assert '$title' value: '$newValue + $addition'") {
        onSendSelectNetworkFeeBottomSheet { inputTextFieldValue(title).assertTextContains(newValue + addition) }
    }
}

fun BaseTestCase.openSendConfirmScreenViaNextButton() {
    step("Click on 'Next' button") {
        onSendAddressScreen {
            addressesShimmer.assertIsNotDisplayed()
            nextButton.assertIsDisplayed()
            nextButton.assertIsEnabled()
            nextButton.performClick()
        }
    }
    step("Assert 'Send' button on 'Send confirm' screen is displayed") {
        onSendConfirmScreen { sendButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.openSendConfirmScreenViaContinueButton() {
    step("Click on 'Continue' button") {
        onSendAddressScreen {
            addressesShimmer.assertIsNotDisplayed()
            continueButton.assertIsDisplayed()
            continueButton.assertIsEnabled()
            continueButton.performClick()
        }
    }
    step("Assert 'Send' button on 'Send confirm' screen is displayed") {
        onSendConfirmScreen { sendButton.assertIsDisplayed() }
    }
}

fun BaseTestCase.openSendSuccessScreenViaLongClickOnSendButton() {
    step("Long click on 'Send' button") {
        onSendConfirmScreen {
            waitForIdle()
            sendButton.assertIsEnabled()
            sendButton.performTouchInput { longClick(durationMillis = HOLD_DURATION_MS) }
        }
    }
    step("Assert 'Transaction sent' screen is displayed") {
        onSendSuccessScreen { container.assertIsDisplayed() }
    }
}

fun BaseTestCase.checkSendViaSwapSuccessScreen() {
    step("Assert 'Transaction sent' title is displayed") {
        onSendSuccessScreen { title.assertIsDisplayed() }
    }
    step("Assert 'Transaction date' is displayed") {
        onSendSuccessScreen { transactionDate.assertIsDisplayed() }
    }
    step("Assert 'Send from' block is displayed") {
        onSendSuccessScreen { sendFromBlock.assertIsDisplayed() }
    }
    step("Assert 'Amount to receive' block is displayed") {
        onSendSuccessScreen { sendToAmountBlock.assertIsDisplayed() }
    }
    step("Assert 'Provider' block is displayed") {
        onSendSuccessScreen { providerBlock.assertIsDisplayed() }
    }
    step("Assert 'Recipient address' block is displayed") {
        onSendSuccessScreen { recipientAddressBlock.assertIsDisplayed() }
    }
    step("Assert 'Network fee' block is displayed") {
        onSendSuccessScreen { feeBlock.assertIsDisplayed() }
    }
    step("Assert 'Explore' button is displayed") {
        onSendSuccessScreen { exploreButton.assertIsDisplayed() }
    }
    step("Assert 'Share' button is displayed") {
        onSendSuccessScreen { shareButton.assertIsDisplayed() }
    }
    step("Assert 'Close' button is displayed") {
        onSendSuccessScreen { closeButton.assertIsDisplayed() }
    }
}

/** From the token details screen, open the transfer bottom sheet and reach the send amount input. */
fun BaseTestCase.openSendFromTokenDetails() {
    step("Click on 'Transfer' button") {
        onTokenDetailsScreen { transferButton.clickWithAssertion() }
    }
    step("Click on 'Send' button in bottom sheet") {
        onTransferBottomSheet { sendButton.clickWithAssertion() }
    }
}

/** Open an existing hot wallet and reach the send amount input for [tokenName]. */
fun BaseTestCase.openSendScreenWithHotWallet(seedPhrase: String, tokenName: String) {
    step("Open 'Main' screen with existing hot wallet") {
        openMainScreenWithExistingHotWallet(seedPhrase)
    }
    step("Click on token with name: '$tokenName'") {
        onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
    }
    openSendFromTokenDetails()
}

fun BaseTestCase.getNetworkFeeAmount(): String {
    var fee = ""
    step("Read current network fee amount") {
        onSendConfirmScreen { fee = feeAmount.extractText() }
    }
    return fee
}

fun BaseTestCase.switchFeeToFastAndApply() {
    val fastOption = getResourceString(R.string.common_fee_selector_option_fast)
    step("Click on fee selector icon") {
        onSendConfirmScreen { feeSelectorIcon.performClick() }
    }
    // Selecting a non-custom speed auto-applies and closes the fee selector — no 'Done' step.
    step("Click on '$fastOption' fee option") {
        onSendSelectNetworkFeeBottomSheet { regularFeeSelectorItem(fastOption).performClick() }
    }
}

fun BaseTestCase.assertNetworkFeeChanged(previousFee: String) {
    step("Assert network fee changed from '$previousFee'") {
        composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            runCatching {
                var current = previousFee
                onSendConfirmScreen { current = feeAmount.extractText() }
                current != previousFee
            }.getOrDefault(false)
        }
    }
}

/** Reads the network fee amount on the 'Send confirm' screen (empty while it shows a loading shimmer). */
fun BaseTestCase.readNetworkFeeAmount(): String {
    var fee = ""
    onSendConfirmScreen { fee = feeAmount.extractText() }
    return fee
}

/**
 * Wait until the network fee value stops changing across two checks — the send button stays disabled
 * (and the hold-to-confirm gesture is swallowed) until the fee re-fetch settles. The hold button has
 * no enabled/disabled semantics, so waiting on the fee value is the only reliable readiness signal.
 */
fun TestContext<Unit>.waitUntilNetworkFeeIsStable(readFee: () -> String) {
    step("Wait for the network fee to finish loading") {
        var previousFee: String? = null
        flakySafely(timeoutMs = WAIT_UNTIL_TIMEOUT_LONG, intervalMs = FEE_STABILITY_INTERVAL_MS) {
            val currentFee = readFee()
            val isStable = currentFee.isNotEmpty() && currentFee == previousFee
            previousFee = currentFee
            if (!isStable) throw AssertionError("Network fee is still settling (current='$currentFee')")
        }
    }
}

private const val FEE_STABILITY_INTERVAL_MS = 750L

fun BaseTestCase.assertNetworkFeeContains(currencySymbol: String) {
    step("Assert network fee contains '$currencySymbol'") {
        onSendConfirmScreen { feeAmount.assertTextContains(currencySymbol, substring = true) }
    }
}

fun BaseTestCase.selectTokenToSendViaSwap(
    swapTokenName: String,
    networkName: String,
    networkType: String? = null,
) {
    step("Click on 'Transfer' button") {
        onTokenDetailsScreen { transferButton.clickWithAssertion() }
    }
    step("Click on 'Send' button in bottom sheet") {
        onTransferBottomSheet { sendButton.clickWithAssertion() }
    }
    step("Click on 'Swap to another token' button") {
        onSendScreen { swapToAnotherTokenButton.performClick() }
    }
    step("Click on token: '$swapTokenName'") {
        onSendViaSwapScreen { tokenItem(swapTokenName).performClick() }
    }
    val networkLabel = if (networkType.isNullOrBlank()) networkName else "$networkName $networkType"
    step("Click on '$networkLabel' network") {
        onChooseNetworkBottomSheet { networkItem(networkName, networkType).performClick() }
    }
}