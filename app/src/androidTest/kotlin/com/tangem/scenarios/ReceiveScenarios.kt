package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onReceiveAssetsBottomSheet
import com.tangem.screens.onTokenReceiveQrCodeBottomSheet
import com.tangem.screens.onTokenReceiveWarningBottomSheet
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.goToQrCodeBottomSheet() {
    step("Assert 'Token receive warning' bottom sheet is displayed") {
        onTokenReceiveWarningBottomSheet { bottomSheet.assertIsDisplayed() }
    }
    step("Click on 'Got it' button") {
        onTokenReceiveWarningBottomSheet { gotItButton.performClick() }
    }
    step("Click on 'Show QR code' button") {
        onReceiveAssetsBottomSheet { showQrCodeButton.clickWithAssertion() }
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
