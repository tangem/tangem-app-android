package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.screens.AlreadyUsedWalletDialogPageObject
import com.tangem.screens.AlreadyUsedWalletDialogPageObject.cancelButton
import com.tangem.screens.AlreadyUsedWalletDialogPageObject.message
import com.tangem.screens.AlreadyUsedWalletDialogPageObject.requestSupportButton
import com.tangem.screens.AlreadyUsedWalletDialogPageObject.thisIsMyWalletButton
import com.tangem.screens.AlreadyUsedWalletDialogPageObject.title
import com.tangem.screens.ScanWarningDialogPageObject
import com.tangem.screens.onFailedTransactionDialog
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.checkFailedTransactionDialog() {
    step("Assert failed transaction dialog is displayed") {
        onFailedTransactionDialog { dialogContainer.assertIsDisplayed() }
    }
    step("Assert failed transaction dialog title is displayed") {
        onFailedTransactionDialog { title.assertIsDisplayed() }
    }
    step("Assert failed transaction dialog text is displayed") {
        onFailedTransactionDialog { text.assertIsDisplayed() }
    }
    step("Assert 'Cancel' button is displayed") {
        onFailedTransactionDialog { cancelButton.assertIsDisplayed() }
    }
    step("Assert 'Support' button is displayed") {
        onFailedTransactionDialog { supportButton.assertIsDisplayed() }
    }
}

fun checkScanWarningDialog() {
    step("Assert 'Scan warning' dialog title is displayed") {
        ScanWarningDialogPageObject { warningTitle.isDisplayed() }
    }
    step("Assert warning dialog message is displayed") {
        ScanWarningDialogPageObject { warningMessage.isDisplayed() }
    }
    step("Assert 'Cancel' button is displayed") {
        ScanWarningDialogPageObject { cancelButton.isDisplayed() }
    }
    step("Assert 'How to scan' button is displayed") {
        ScanWarningDialogPageObject { howToScanButton.isDisplayed() }
    }
    step("Assert 'Request support' button is displayed") {
        ScanWarningDialogPageObject { requestSupportButton.isDisplayed() }
    }
}

fun checkAlreadyUsedWalletDialog() {
    step("Assert 'Already used Wallet' dialog title is displayed") {
        AlreadyUsedWalletDialogPageObject { title.isDisplayed() }
    }
    step("Assert dialog message is displayed") {
        AlreadyUsedWalletDialogPageObject { message.isDisplayed() }
    }
    step("Assert 'This is my wallet' button is displayed") {
        AlreadyUsedWalletDialogPageObject { thisIsMyWalletButton.isDisplayed() }
    }
    step("Assert 'Cancel' button is displayed") {
        AlreadyUsedWalletDialogPageObject { cancelButton.isDisplayed() }
    }
    step("Assert 'Request support' button is displayed") {
        AlreadyUsedWalletDialogPageObject { requestSupportButton.isDisplayed() }
    }
}