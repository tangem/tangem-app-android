package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.utils.decodeQrCode
import com.tangem.screens.onAddFundsBottomSheet
import com.tangem.screens.onReceiveAssetsBottomSheet
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTokenReceiveQrCodeBottomSheet
import com.tangem.screens.onTokenReceiveWarningBottomSheet
import io.qameta.allure.kotlin.Allure.step
import org.junit.Assert

/** Opens the receive flow from a funded token's details via 'Add funds' → 'Receive'. */
fun BaseTestCase.openReceiveViaAddFunds() {
    step("Click on 'Add funds' button") {
        onTokenDetailsScreen { addFundsButton.clickWithAssertion() }
    }
    step("Click on 'Receive' button in bottom sheet") {
        onAddFundsBottomSheet { receiveButton.clickWithAssertion() }
    }
}

/**
 * Asserts the QR code encodes the displayed address for both address types of a two-address-type coin,
 * and that the two addresses differ.
 */
fun BaseTestCase.assertQrCodesMatchForBothAddressTypes() {
    step("Go to QR code bottom sheet for the first address type") {
        goToQrCodeBottomSheet()
    }
    var firstAddress = ""
    step("Assert QR code encodes the first displayed address") {
        firstAddress = assertQrCodeEncodesDisplayedAddress()
    }
    step("Go back to the receive addresses") {
        device.uiDevice.pressBack()
    }
    step("Switch to the second address type") {
        awaitSuccess(WAIT_UNTIL_TIMEOUT_LONG) { onReceiveAssetsBottomSheet { addressesPager.assertIsDisplayed() } }
        onReceiveAssetsBottomSheet { scrollToAddress(1) }
    }
    step("Click on 'Show QR code' button for the second address type") {
        onReceiveAssetsBottomSheet { showQrCodeButton(1).clickWithAssertion() }
    }
    var secondAddress = ""
    step("Assert QR code encodes the second displayed address") {
        secondAddress = assertQrCodeEncodesDisplayedAddress()
    }
    step("Assert the two address types are different") {
        Assert.assertNotEquals(firstAddress, secondAddress)
    }
}

fun BaseTestCase.goToQrCodeBottomSheet() {
    step("Assert 'Token receive warning' bottom sheet is displayed") {
        onTokenReceiveWarningBottomSheet { bottomSheet.assertIsDisplayed() }
    }
    step("Click on 'Got it' button") {
        onTokenReceiveWarningBottomSheet { gotItButton.performClick() }
    }
    step("Click on 'Show QR code' button") {
        onReceiveAssetsBottomSheet { showQrCodeButton().clickWithAssertion() }
    }
}

fun BaseTestCase.checkQrCodeBottomSheetScenario() {
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

/** Decodes the QR code on the receive bottom sheet and asserts it encodes the displayed address; returns that address. */
fun BaseTestCase.assertQrCodeEncodesDisplayedAddress(): String {
    var displayedAddress = ""
    step("Assert QR code encodes the displayed address") {
        onTokenReceiveQrCodeBottomSheet {
            displayedAddress = address.extractText()
            val decoded = decodeQrCode(captureQrCodeBitmap())
            Assert.assertEquals(displayedAddress, decoded)
        }
    }
    return displayedAddress
}
