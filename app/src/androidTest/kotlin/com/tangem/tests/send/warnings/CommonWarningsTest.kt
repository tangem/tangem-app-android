package com.tangem.tests.send.warnings

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.scenarios.checkSendWarning
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openSendConfirmScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onSendConfirmScreen
import com.tangem.screens.onSendSelectNetworkFeeBottomSheet
import com.tangem.wallet.R
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class CommonWarningsTest : BaseTestCase() {

    @AllureId("4293")
    @DisplayName("Warnings: check warning, when custom fee lower than 'Slow'")
    @Test
    fun warningDisplayedWhenCustomFeeLowerThanSlowTest() {
        val tokenName = "Ethereum"
        val sendAmount = "0.01"
        val feeUpTo = getResourceString(R.string.send_max_fee)
        val customFee = "0.000000001"
        val warningTitle = getResourceString(R.string.send_notification_transaction_delay_title)
        val warningMessage = getResourceString(R.string.send_notification_transaction_delay_text)

        setupHooks().run {

            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Confirm' screen with token: $tokenName") {
                openSendConfirmScreen(tokenName, sendAmount, ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Click on fee selector icon") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendConfirmScreen { feeSelectorIcon.performClick() }
                    onSendSelectNetworkFeeBottomSheet { customSelectorItem.assertIsDisplayed() }
                }
            }
            step("Click on 'Custom' selector item") {
                onSendSelectNetworkFeeBottomSheet { customSelectorItem.performClick() }
            }
            step("Type '$customFee' into input text field") {
                onSendSelectNetworkFeeBottomSheet {
                    inputTextFieldValue(title = feeUpTo).performClick()
                    inputTextFieldValue(title = feeUpTo).performTextReplacement(customFee)
                }
            }
            step("Click on 'Done' button") {
                onSendSelectNetworkFeeBottomSheet { doneButton.performClick() }
            }
            step("Assert 'Transaction delays are possible' warning is displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = true,
                    sendButtonIsDisabled = false
                )
            }
        }
    }

    @AllureId("4221")
    @DisplayName("Warnings: check warning, when amount exceeds total balance")
    @Test
    fun warningDisplayedWhenAmountExceedsTotalBalanceTest() {
        val tokenName = "Ethereum"
        val sendAmount = "0.9999"
        val network = "ETH 0.00032"
        val amount = "\$0.81"
        val warningTitle = getResourceString(R.string.send_network_fee_warning_title)
        val warningMessage = getResourceString(R.string.common_network_fee_warning_content, network, amount)

        setupHooks().run {

            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    synchronizeAddresses()
                }
            }
            step("Open 'Send Confirm' screen with token: $tokenName") {
                openSendConfirmScreen(tokenName, sendAmount, ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Assert 'Network fee coverage' warning is displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = true,
                    sendButtonIsDisabled = false
                )
            }
        }
    }

    @AllureId("4294")
    @DisplayName("Warnings: check warning, when custom fee is high")
    @Test
    fun warningDisplayedWhenCustomFeeIsHighTest() {
        val tokenName = "Ethereum"
        val sendAmount = "0.01"
        val feeUpTo = getResourceString(R.string.send_max_fee)
        val customFee = "0.004"
        val timesHigher = "7"
        val warningTitle = getResourceString(R.string.send_notification_fee_too_high_title)
        val warningMessage = getResourceString(R.string.send_notification_fee_too_high_text, timesHigher)

        setupHooks().run {

            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Send Confirm' screen with token: $tokenName") {
                openSendConfirmScreen(tokenName, sendAmount, ETHEREUM_RECIPIENT_ADDRESS)
            }
            step("Click on fee selector icon") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onSendConfirmScreen { feeSelectorIcon.performClick() }
                    onSendSelectNetworkFeeBottomSheet { customSelectorItem.assertIsDisplayed() }
                }
            }
            step("Click on 'Custom' selector item") {
                onSendSelectNetworkFeeBottomSheet { customSelectorItem.performClick() }
            }
            step("Type '$customFee' into input text field") {
                onSendSelectNetworkFeeBottomSheet {
                    inputTextFieldValue(title = feeUpTo).performClick()
                    inputTextFieldValue(title = feeUpTo).performTextReplacement(customFee)
                }
            }
            step("Click on 'Done' button") {
                onSendSelectNetworkFeeBottomSheet { doneButton.performClick() }
            }
            step("Assert 'Custom fee is high' warning is displayed") {
                checkSendWarning(
                    title = warningTitle,
                    message = warningMessage,
                    isDisplayed = true,
                    sendButtonIsDisabled = false
                )
            }
        }
    }
}