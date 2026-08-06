package com.tangem.tests.tangempay

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.platform.app.InstrumentationRegistry
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.utils.assertClipboardTextEquals
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.features.tangempay.TangemPayConstants
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.hamcrest.CoreMatchers.allOf
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class TangemPayTest : BaseTestCase() {

    @AllureId("4549")
    @DisplayName("Tangem Pay: change PIN code from card details")
    @Test
    fun changePinSetsNewPinCodeFromCardDetailsTest() {
        val newPin = "5217"
        val pinSetupScenario = "tangem_pay_pin_setup"
        val pinNotSetState = "PinNotSet"
        val eligibilityState = "PaeraCustomer"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(pinSetupScenario, pinNotSetState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(pinSetupScenario)
            },
        ).run {
            step("Open PIN entry screen") { openTangemPayChangePin() }
            step("Enter PIN '$newPin'") {
                onTangemPayChangePinScreen { inputField.performTextInput(newPin) }
            }
            step("Assert success screen title is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayChangePinScreen { successTitle.assertIsDisplayed() }
                }
            }
            step("Click on 'Done' button") {
                onTangemPayChangePinScreen { doneButton.clickWithAssertion() }
            }
        }
    }

    @AllureId("4969")
    @DisplayName("Tangem Pay: balance updates after transaction on payment account screen")
    @Test
    fun balanceUpdatesAfterTransactionOnPaymentAccountScreenTest() {
        val balanceScenario = "tangem_pay_balance_update"
        val initialState = "InitialBalance"
        val afterTransactionState = "AfterTransaction"
        val eligibilityState = "PaeraCustomer"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(balanceScenario, initialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(balanceScenario)
            },
        ).run {
            openTangemPay()
            step("Assert initial balance contains '10'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe("10", substring = true) }
            }
            step("Switch WireMock scenario '$balanceScenario' to '$afterTransactionState'") {
                setWireMockScenarioState(balanceScenario, afterTransactionState)
            }
            step("Pull to refresh") { pullToRefreshTangemPay() }
            step("Assert updated balance contains '9'") {
                onTangemPayMainScreen { balance.assertTextContainsSafe("9", substring = true) }
            }
        }
    }

    @AllureId("4970")
    @DisplayName("Tangem Pay: new transaction appears after mocked charge")
    @Test
    fun transactionListNewTransactionAppearsAfterMockedChargeTest() {
        val historyScenario = "tangem_pay_transaction_history"
        val initialState = "InitialEmpty"
        val afterTransactionState = "AfterTransaction"
        val eligibilityState = "PaeraCustomer"
        val merchantName = "Mock Merchant"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(historyScenario, initialState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(historyScenario)
            },
        ).run {
            openTangemPay()
            step("Assert transaction from '$merchantName' is not displayed") {
                onTangemPayMainScreen {
                    transactionRowWithText(merchantName).assertDoesNotExist()
                }
            }
            step("Switch WireMock scenario '$historyScenario' to '$afterTransactionState'") {
                setWireMockScenarioState(historyScenario, afterTransactionState)
            }
            step("Pull to refresh") { pullToRefreshTangemPay() }
            step("Assert transaction from '$merchantName' is displayed") {
                onTangemPayMainScreen {
                    transactionRowWithText(merchantName).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("4974")
    @DisplayName("Tangem Pay: reveal and copy card number, expiration and CVC")
    @Test
    fun revealAndCopyCardDetailsNumberExpirationCVCTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val eligibilityState = "PaeraCustomer"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
            },
        ).run {
            openTangemPay()
            step("Click on card button") {
                waitForIdle()
                onTangemPayMainScreen { cardButton.clickWithAssertion() }
            }
            step("Click on 'Show details' button") {
                waitForIdle()
                onTangemPayCardPageScreen { showDetailsButton.clickWithAssertion() }
            }
            step("Assert number, expiration and CVC values are visible") {
                onTangemPayCardPageScreen {
                    numberValue.assertIsDisplayed()
                    expirationValue.assertIsDisplayed()
                    cvcValue.assertIsDisplayed()
                }
            }
            var displayedNumber = ""
            var displayedExpiration = ""
            var displayedCvc = ""
            onTangemPayCardPageScreen {
                displayedNumber = numberValue.extractText()
                displayedExpiration = expirationValue.extractText()
                displayedCvc = cvcValue.extractText()
            }
            step("Click on 'Copy card number' button") {
                onTangemPayCardPageScreen { copyNumberButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert clipboard contains card number") {
                // Displayed number has spaces for readability; clipboard copies digits only.
                assertClipboardTextEquals(displayedNumber.replace(" ", ""), context)
            }
            step("Click on 'Copy expiration' button") {
                onTangemPayCardPageScreen { copyExpirationButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert clipboard contains expiration date") {
                assertClipboardTextEquals(displayedExpiration, context)
            }
            step("Click on 'Copy CVC' button") {
                onTangemPayCardPageScreen { copyCvcButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Assert clipboard contains CVC") {
                assertClipboardTextEquals(displayedCvc, context)
            }
        }
    }

    @AllureId("4971")
    @DisplayName("Tangem Pay: freeze card via confirmation sheet")
    @Test
    fun freezeUnfreezeCardTogglesCardStateTest() {
        val freezeScenario = "tangem_pay_card_freeze"
        val startedState = "Started"
        val eligibilityState = "PaeraCustomer"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(freezeScenario, startedState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(freezeScenario)
            },
        ).run {
            openTangemPay()
            step("Click on card button") {
                onTangemPayMainScreen { cardButton.clickWithAssertion() }
            }
            step("Click on freeze card row (card is active)") {
                onTangemPayCardPageScreen { freezeCardRow.clickWithAssertion() }
            }
            step("Assert freeze confirmation sheet is displayed") {
                onTangemPayFreezeConfirmation { freezeTitle.assertIsDisplayed() }
            }
            step("Click on 'Submit' button (confirm freeze)") {
                onTangemPayFreezeConfirmation { submitButton.clickWithAssertion() }
            }
            step("Assert frozen badge is displayed") {
                onTangemPayCardPageScreen { cardFrozenBadge.assertIsDisplayed() }
            }
        }
    }

    @AllureId("9592")
    @DisplayName("Tangem Pay: Terms and fees opens the tariffs document")
    @Test
    fun termsAndFeesOpensTariffsFromMoreActionsMenuTest() {
        val eligibilityState = "PaeraCustomer"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
            },
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Click on 'More actions' button") {
                onTangemPayMainScreen { moreActionsButton.clickWithAssertion() }
            }
            step("Stub external browser so the tariffs document is not actually opened") {
                intending(hasAction(Intent.ACTION_VIEW))
                    .respondWith(ActivityResult(Activity.RESULT_OK, null))
            }
            step("Click on 'Terms and fees' menu item") {
                onTangemPayMainScreen { termsAndFeesMenuItem.clickWithAssertion() }
            }
            step("Assert the tariffs document is opened in an external browser") {
                intended(allOf(hasAction(Intent.ACTION_VIEW), hasData(TangemPayConstants.TERMS_AND_LIMITS_LINK)))
            }
        }
    }

    // iOS-only: Android has no 'Service temporarily unavailable' sheet and no empty-depositAddress guard in Add funds.
    @Ignore("[REDACTED_JIRA]")
    @AllureId("9557")
    @DisplayName("Tangem Pay: service unavailable error when customer has no deposit address")
    @Test
    fun addFundsShowsServiceUnavailableErrorWhenNoDepositAddressTest() {
        val depositAddressScenario = "tangem_pay_deposit_address"
        val noDepositAddressState = "NoDepositAddress"
        val eligibilityState = "PaeraCustomer"

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(depositAddressScenario, noDepositAddressState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(depositAddressScenario)
            },
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Click on 'Add funds' button") {
                onTangemPayMainScreen { topUpButton.clickWithAssertion() }
            }
            step("Assert 'Service temporarily unavailable' sheet is displayed") {
                onTangemPayServiceUnavailableSheet {
                    title.assertIsDisplayed()
                    description.assertIsDisplayed()
                    gotItButton.assertIsDisplayed()
                }
            }
            step("Click on 'Got it' button") {
                onTangemPayServiceUnavailableSheet { gotItButton.clickWithAssertion() }
            }
            step("Assert 'Service temporarily unavailable' sheet is not displayed") {
                onTangemPayServiceUnavailableSheet { title.assertDoesNotExist() }
            }
        }
    }
}