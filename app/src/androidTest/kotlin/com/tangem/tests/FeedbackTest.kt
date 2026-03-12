package com.tangem.tests

import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.clickAndWaitFor
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.redux.StateDialog
import com.tangem.scenarios.checkFailedTransactionDialog
import com.tangem.scenarios.checkScanWarningDialog
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.sdk.mocks.MockProvider
import com.tangem.tap.store
import dagger.hilt.android.testing.HiltAndroidTest
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
        val gmailText = "Welcome to Gmail"

        setupHooks(
            additionalAfterSection = {
                device.uiDevice.pressBack()
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click 'More' button on TopBar") {
                waitForIdle()
                onTopBar { moreButton.clickWithAssertion() }
            }
            step("Click 'Contact support' button") {
                waitForIdle()
                onDetailsScreen { contactSupportButton.clickWithAssertion() }
            }
            step("Assert 'Gmail' app is open") {
                ThirdPartyAppPageObject { assertElementWithTextExists(gmailText) }
            }
        }
    }

    @Ignore("TODO: [REDACTED_JIRA]")
    @AllureId("893")
    @DisplayName("Send feedback: failed transaction")
    @Test
    fun sendFeedbackFromFailedTransactionTest() {
        val tokenName = "Polygon"
        val recipientAddress = RECIPIENT_ADDRESS
        val sendAmount = "1"
        val gmailText = "Welcome to Gmail"

        setupHooks(
            additionalAfterSection = {
                device.uiDevice.pressBack()
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on token with name: '$tokenName'") {
                onMainScreen { tokenWithTitleAndAddress(tokenName).clickWithAssertion() }
            }
            step("Click 'Send' button") {
                onTokenDetailsScreen { sendButton().performClick() }
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
                onSendAddressScreen { addressTextField.performTextInput(recipientAddress) }
            }
            step("Assert text field contains text: $recipientAddress") {
                onSendAddressScreen { addressTextField.assertTextEquals(recipientAddress) }
            }
            step("Click 'Next' button") {
                onSendAddressScreen {
                    nextButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onSendConfirmScreen { sendingText.assertIsDisplayed() }
                        }
                    )
                }
            }
            step("Click 'Send' button") {
                waitForIdle()
                onSendConfirmScreen {
                    sendButton.assertIsEnabled()
                    sendButton.clickAndWaitFor(
                        rule = composeTestRule,
                        expectedCondition = {
                            onFailedTransactionDialog { dialogContainer.assertIsDisplayed() }
                        }
                    )
                }
            }
            step("Check 'Failed transaction' dialog") {
                waitForIdle()
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    checkFailedTransactionDialog()
                }
            }
            step("Click on 'Support' button") {
                onFailedTransactionDialog { supportButton.performClick() }
            }
            step("Assert 'Gmail' app is open") {
                ThirdPartyAppPageObject { assertElementWithTextExists(gmailText) }
            }
        }
    }

    @AllureId("3985")
    @DisplayName("Send feedback: from 'Warning' dialog after card scan")
    @Test
    fun sendFeedbackFromScanScreenTest() {
        val gmailText = "Welcome to Gmail"

        setupHooks(
            additionalAfterSection = {
                device.uiDevice.pressBack()
                MockProvider.resetEmulateError()
            }
        ).run {
            step("Click on 'Accept' button") {
                onDisclaimerScreen { acceptButton.clickWithAssertion() }
            }
            step("Set scanning error") {
                MockProvider.setEmulateError(TangemSdkError.TagLost())
            }
            step("Click on 'Get started' button") {
                onStoriesScreen { getStartedButton.clickWithAssertion() }
            }
            step("Click on 'Scan card or ring' button") {
                onCreateWalletStartScreen { scanCardOrRingButton.clickWithAssertion() }
            }
            step("Force show 'Scan warning' dialog"){
                runOnUiThread {
                    val scanFailsState = StateDialog.ScanFailsDialog(source = StateDialog.ScanFailsSource.MAIN)
                    store.dispatch(GlobalAction.ShowDialog(scanFailsState))
                }
            }
            step("Check 'Scan warning' dialog") {
                waitForIdle()
                checkScanWarningDialog()
            }
            step("Click on 'Request support' button") {
                ScanWarningDialogPageObject { requestSupportButton.click() }
            }
            step("Assert 'Gmail' app is open") {
                ThirdPartyAppPageObject { assertElementWithTextExists(gmailText) }
            }
        }
    }
}