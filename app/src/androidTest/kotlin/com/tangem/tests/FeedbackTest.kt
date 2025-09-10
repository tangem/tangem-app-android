package com.tangem.tests

import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.redux.StateDialog
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.sdk.mocks.MockProvider
import com.tangem.tap.store
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.Allure
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class FeedbackTest : BaseTestCase() {

    @AllureId("894")
    @DisplayName("Send feedback: from details")
    @Test
    fun sendFeedbackFromDetailsTest() {
        setupHooks(
            additionalAfterSection = {
                device.uiDevice.pressBack()
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Click 'More' button on TopBar") {
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click 'Contact support' button") {
                onDetailsScreen { contactSupportButton.clickWithAssertion() }
            }
            step("Check 'Contact support' intent is called") {
                checkSendEMailIntentCalled()
            }
        }
    }

    @AllureId("893")
    @DisplayName("Send feedback: failed transaction")
    @Test
    fun sendFeedbackFromFailedTransactionTest() {
        val balance = TOTAL_BALANCE
        val tokenName = "Polygon"
        val recipientAddress = RECIPIENT_ADDRESS
        val sendAmount = "1"

        setupHooks(
            additionalAfterSection = {
                device.uiDevice.pressBack()
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click 'Send' button") {
                onTokenDetailsScreen { sendButton.performClick() }
            }
            step("Type '$sendAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(sendAmount)
                }
            }
            step("Assert input text field has value: '$sendAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(value = sendAmount, substring = true) }
            }
            step("Click 'Next' button") {
                onSendScreen { nextButton.clickWithAssertion() }
            }
            step("Enter address") {
                onSendAddressScreen { addressTextField.performTextReplacement(recipientAddress) }
            }
            step("Click 'Next' button") {
                onSendAddressScreen { nextButton.clickWithAssertion() }
            }
            step("Click 'Send' button") {
                onSendConfirmScreen { sendButton.clickWithAssertion() }
            }
            step("Check 'Failed transaction' dialog") {
                checkFailedTransactionDialog()
            }
            step("Click on 'Support' button") {
                onFailedTransactionDialog { supportButton.performClick() }
            }
            step("Check 'Contact support' intent is called") {
                checkSendEMailIntentCalled()
            }
        }
    }

    @AllureId("3985")
    @DisplayName("Send feedback: from 'Warning' dialog after card scan")
    @Test
    fun sendFeedbackFromScanScreenTest() {
        setupHooks(
            additionalAfterSection = {
                device.uiDevice.pressBack()
                MockProvider.resetEmulateError()
            }
        ).run {
            Allure.step("Click on 'Accept' button") {
                onDisclaimerScreen { acceptButton.clickWithAssertion() }
            }
            step("Set scanning error") {
                MockProvider.setEmulateError(TangemSdkError.TagLost())
            }
            step("Click on 'Scan' button") {
                onStoriesScreen { scanButton.performClick() }
            }
            step("Force show 'Scan warning' dialog"){
                runOnUiThread {
                    val scanFailsState = StateDialog.ScanFailsDialog(source = StateDialog.ScanFailsSource.MAIN)
                    store.dispatch(GlobalAction.ShowDialog(scanFailsState))
                }
            }
            step("Check 'Scan warning' dialog") {
                checkScanWarningDialog()
            }
            step("Click on 'Request support' button") {
                ScanWarningDialogPageObject { requestSupportButton.click() }
            }
            step("Check 'Contact support' intent is called") {
                checkSendEMailIntentCalled()
            }
        }
    }

    @AllureId("3986")
    @DisplayName("Send feedback: from scan already used wallet alert dialog")
    @Ignore("TODO On CI Already used wallet doesn't displayed")
    @Test
    fun sendFeedbackAfterScanAlreadyUsedWalletTest() {
        setupHooks(
            additionalAfterSection = {
                device.uiDevice.pressBack()
            }
        ).run {
            step("Set mocks for Wallet2") {
                MockProvider.setMocks(ProductType.Wallet2)
            }
            step("Click on 'Accept' button") {
                onDisclaimerScreen { acceptButton.clickWithAssertion() }
            }
            step("Click on 'Scan' button") {
                onStoriesScreen { scanButton.clickWithAssertion() }
            }
            step("Check 'Already used Wallet' dialog") {
                checkAlreadyUsedWalletDialog()
            }
            step("Click on 'Request support' button") {
                AlreadyUsedWalletDialogPageObject { requestSupportButton.click() }
            }
            step("Check 'Contact support' intent is called") {
                checkSendEMailIntentCalled()
            }
        }
    }
}