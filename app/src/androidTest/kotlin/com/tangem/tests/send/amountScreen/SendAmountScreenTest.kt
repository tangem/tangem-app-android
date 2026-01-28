package com.tangem.tests.send.amountScreen

import android.view.KeyEvent
import com.tangem.common.BaseTestCase
import com.tangem.common.utils.setClipboardText
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.wallet.R
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SendAmountScreenTest : BaseTestCase() {

    @AllureId("4761")
    @DisplayName("Send (amount screen): validate different amounts")
    @Test
    fun validateDifferentAmountsTest() {
        val tokenName = "Ethereum"
        val manualSendAmount = "1"
        val clipboardSendAmount = "0.5"
        val invalidAmount = "2"
        val errorText = getResourceString(R.string.send_validation_amount_exceeds_balance)
        val context = device.context

        setupHooks().run {
            step("Open 'Send' screen") {
                openSendScreen(tokenName)
            }
            step("Type '$manualSendAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(manualSendAmount)
                }
            }
            step("Assert send amount = '$manualSendAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(manualSendAmount, substring = true) }
            }
            step("Assert 'Next' button is enabled") {
                onSendScreen { nextButton.assertIsEnabled() }
            }
            step("Press system 'Delete' button") {
                waitForIdle()
                device.uiDevice.pressDelete()
            }
            step("Set clipboard text") {
                setClipboardText(context, clipboardSendAmount)
            }
            step("Click on input text field") {
                onSendScreen { amountInputTextField.performClick() }
            }
            step("Paste from clipboard") {
                device.uiDevice.pressKeyCode(KeyEvent.KEYCODE_V, KeyEvent.META_CTRL_ON)
            }
            step("Assert send amount = '$clipboardSendAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(clipboardSendAmount, substring = true) }
            }
            step("Assert 'Next' button is enabled") {
                onSendScreen { nextButton.assertIsEnabled() }
            }
            step("Press system 'Delete' button") {
                waitForIdle()
                device.uiDevice.pressDelete()
            }
            step("Type '$invalidAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(invalidAmount)
                }
            }
            step("Assert send amount = '$invalidAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(invalidAmount, substring = true) }
            }
            step("Assert amount error contains text '$errorText'") {
                onSendScreen { amountErrorText.assertTextContains(errorText) }
            }
            step("Assert 'Next' button is disabled") {
                onSendScreen { nextButton.assertIsNotEnabled() }
            }
            step("Click on 'Max' button") {
                onSendScreen { maxButton.performClick() }
            }
            step("Assert send amount = '$manualSendAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(manualSendAmount, substring = true) }
            }
            step("Assert 'Next' button is enabled") {
                onSendScreen { nextButton.assertIsEnabled() }
            }
            step("Click on 'Close' button") {
                onSendScreen { closeButton.performClick() }
            }
            step("Assert 'Token Details' screen is displayed") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
        }
    }

    @AllureId("4762")
    @DisplayName("Send (amount screen): switch equivalent")
    @Test
    fun switchEquivalentTest() {
        val tokenName = "Ethereum"
        val tokenAmount = "1"
        val fiatAmount = "$2,535.63"

        setupHooks().run {
            step("Open 'Send' screen") {
                openSendScreen(tokenName)
            }
            step("Type '$tokenAmount' in input text field") {
                onSendScreen {
                    amountInputTextField.performClick()
                    amountInputTextField.performTextReplacement(tokenAmount)
                }
            }
            step("Assert send amount = '$tokenAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(tokenAmount, substring = true) }
            }
            step("Assert equivalent amount = '$fiatAmount'") {
                onSendScreen { equivalentInputAmount.assertTextContains(fiatAmount, substring = true) }
            }
            step("Click on 'Exchange' button") {
                onSendScreen { exchangeIcon.performClick() }
            }
            step("Assert send amount = '$fiatAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(fiatAmount, substring = true) }
            }
            step("Assert equivalent amount = '$tokenAmount'") {
                onSendScreen { equivalentInputAmount.assertTextContains(tokenAmount, substring = true) }
            }
            step("Click on 'Exchange' button") {
                onSendScreen { exchangeIcon.performClick() }
            }
            step("Assert send amount = '$tokenAmount'") {
                onSendScreen { amountInputTextField.assertTextContains(tokenAmount, substring = true) }
            }
            step("Assert equivalent amount = '$fiatAmount'") {
                onSendScreen { equivalentInputAmount.assertTextContains(fiatAmount, substring = true) }
            }
        }
    }
}