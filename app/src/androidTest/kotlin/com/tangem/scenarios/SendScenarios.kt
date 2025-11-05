package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.*
import com.tangem.screens.onMainScreen
import com.tangem.screens.onSendConfirmScreen
import com.tangem.screens.onSendScreen
import com.tangem.screens.onTokenDetailsScreen
import io.qameta.allure.kotlin.Allure.step

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

fun BaseTestCase.checkScanQrScreen() {
    step("Assert 'Back' button is displayed") {
        onScanQrScreen { backTopAppBarButton.assertIsDisplayed() }
    }
    step("Assert 'Flashlight' button is displayed") {
        onScanQrScreen { flashlightButton.assertIsDisplayed() }
    }
    step("Assert 'Gallery' button is displayed") {
        onScanQrScreen { galleryButton.assertIsDisplayed() }
    }
    step("Assert 'Paste from clipboard' is displayed") {
        onScanQrScreen { pasteFromClipboardButton.assertIsDisplayed() }
    }
}