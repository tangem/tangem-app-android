package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.screens.*
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSendConfirmScreen
import com.tangem.screens.onSendScreen
import com.tangem.screens.onTokenDetailsScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openSendScreen(tokenName: String, mockState: String = "") {
    val scenarioState = mockState.ifEmpty { tokenName }
    step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
        setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = scenarioState)
    }
    step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$scenarioState'") {
        setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = scenarioState)
    }
    step("Open 'Main Screen'") {
        openMainScreen()
    }
    step("Synchronize addresses") {
        synchronizeAddresses()
    }
    step("Click on token with name: '$tokenName'") {
        onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
    }
    step("Click on 'Send' button") {
        onTokenDetailsScreen { sendButton().performClick() }
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
    step("Click on 'Next' button") {
        onSendAddressScreen { nextButton.clickWithAssertion() }
    }
}

fun BaseTestCase.openSendAddressScreen(
    tokenName: String,
    inputAmount: String
) {
    step("Click on token with name: '$tokenName'") {
        onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
    }
    step("Click on 'Send' button") {
        onTokenDetailsScreen { sendButton().performClick() }
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