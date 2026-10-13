package com.tangem.scenarios

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankRecipient
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.tapBackButton
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.screens.onHotWalletAccessCodeScreen
import com.tangem.screens.unlockDialogAccessCodeInputMatcher
import com.tangem.screens.tangempay.TangemPayBankWithdrawField
import com.tangem.screens.tangempay.onTangemPayBankRecipientsScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawAmountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawChoiceScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawFormScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawPreviewScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayResidencySelectorScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.setTangemPayBankOfframpScenarios() {
    val channelsBankOfframpState = "BankOfframp"

    setTangemPayMultiNetworkScenarios()
    step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBankOfframpState'") {
        setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBankOfframpState)
    }
}

fun BaseTestCase.openTangemPayWithdrawHub() {
    openTangemPay()
    openWithdrawHubFromPaymentAccount()
}

fun BaseTestCase.openWithdrawHubFromPaymentAccount() {
    step("Click on 'Withdraw' action chip") {
        onTangemPayMainScreen { withdrawButton.clickWithAssertion() }
    }
    step("Assert 'Withdraw' hub screen is displayed") {
        awaitSuccess { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
    }
}

fun BaseTestCase.returnToPaymentAccountFromWithdrawHub() {
    step("Click on 'Back' button") { device.uiDevice.pressBack() }
    step("Assert payment account balance is displayed") {
        awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
    }
}

fun BaseTestCase.openBankRecipientsFromWithdrawHub() {
    step("Click on 'See all' chip") {
        onTangemPayWithdrawHubScreen {
            scrollToSeeAllChip()
            seeAllChip.performClick()
        }
    }
    step("Assert 'Bank transfers' screen is displayed") {
        awaitSuccess { onTangemPayBankRecipientsScreen { title.assertIsDisplayed() } }
    }
}

fun BaseTestCase.searchTangemPayBankRecipients(query: String) {
    step("Enter '$query' into 'Search' field") {
        onTangemPayBankRecipientsScreen { searchField.performTextReplacement(query) }
    }
    step("Dismiss keyboard") {
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }
}

fun BaseTestCase.assertTangemPayRecentRecipientDisplayed(recipient: TangemPayBankRecipient) {
    awaitSuccess { onTangemPayWithdrawHubScreen { scrollToRecentRecipient(recipient.shortName) } }
    onTangemPayWithdrawHubScreen {
        recentRecipient(recipient.initial, recipient.shortName, recipient.accountNumber).assertIsDisplayed()
    }
}

fun BaseTestCase.assertTangemPayBankTransferBlockUnavailable() {
    step("Assert 'Available only for USDC on Polygon, Base, Arbitrum' is displayed") {
        awaitSuccess { onTangemPayWithdrawHubScreen { bankUnavailableSubtitle.assertIsDisplayed() } }
    }
    step("Assert 'To personal account' button is disabled") {
        onTangemPayWithdrawHubScreen { toPersonalAccountButton.assertIsNotEnabled() }
    }
    step("Assert 'To business account' button is disabled") {
        onTangemPayWithdrawHubScreen { toBusinessAccountButton.assertIsNotEnabled() }
    }
}

fun BaseTestCase.enterTangemPayBankWithdrawAmount(amount: String) {
    step("Enter amount '$amount'") {
        awaitSuccess { onTangemPayBankWithdrawAmountScreen { amountInput.assertIsDisplayed() } }
        onTangemPayBankWithdrawAmountScreen { amountInput.performTextReplacement(amount) }
    }
    step("Dismiss keyboard") {
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }
}

fun BaseTestCase.openTangemPayBankWithdrawSourceSelector() {
    step("Dismiss keyboard") {
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }
    step("Click on the source token row") {
        onTangemPayBankWithdrawAmountScreen { sourceRow.performClick() }
    }
    step("Assert 'Choose network' sheet is displayed") {
        awaitSuccess { onTangemPayBankWithdrawAmountScreen { tokenSheetTitle.assertIsDisplayed() } }
    }
}

fun BaseTestCase.returnToPaymentAccountFromBankWithdrawSourceSelector() {
    step("Close 'Choose network' sheet") { device.uiDevice.pressBack() }
    step("Assert 'Choose network' sheet is not displayed") {
        awaitSuccess { onTangemPayBankWithdrawAmountScreen { tokenSheetTitle.assertDoesNotExist() } }
    }
    step("Click on 'Back' button") { tapBackButton() }
    step("Assert 'Withdraw' hub screen is displayed") {
        awaitSuccess { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
    }
    returnToPaymentAccountFromWithdrawHub()
}

fun BaseTestCase.clickTangemPayBankWithdrawAmountContinue() {
    awaitSuccess { onTangemPayBankWithdrawAmountScreen { continueButton.assertIsEnabled() } }
    onTangemPayBankWithdrawAmountScreen { continueButton.performClick() }
}

fun BaseTestCase.openTangemPayBankWithdrawToPersonalAccount() {
    awaitSuccess { onTangemPayWithdrawHubScreen { toPersonalAccountButton.assertIsEnabled() } }
    onTangemPayWithdrawHubScreen { toPersonalAccountButton.performClick() }
}

fun BaseTestCase.openTangemPayBankWithdrawToBusinessAccount() {
    awaitSuccess { onTangemPayWithdrawHubScreen { toBusinessAccountButton.assertIsEnabled() } }
    onTangemPayWithdrawHubScreen { toBusinessAccountButton.performClick() }
}

fun BaseTestCase.chooseTangemPayBankWithdrawOption(sectionTitle: String, option: String) {
    step("Assert '$sectionTitle' step is displayed") {
        awaitSuccess { onTangemPayBankWithdrawChoiceScreen { sectionTitle(sectionTitle).assertIsDisplayed() } }
    }
    step("Click on '$option' option") {
        onTangemPayBankWithdrawChoiceScreen { option(option).performClick() }
    }
    step("Click on 'Continue' button") {
        awaitSuccess { onTangemPayBankWithdrawChoiceScreen { continueButton.assertIsEnabled() } }
        onTangemPayBankWithdrawChoiceScreen { continueButton.performClick() }
    }
}

fun BaseTestCase.fillTangemPayBankRecipientDetails(recipient: TangemPayBankRecipient) {
    step("Assert recipient details step is displayed") {
        awaitSuccess {
            onTangemPayBankWithdrawFormScreen { field(TangemPayBankWithdrawField.AccountNumber).assertIsDisplayed() }
        }
    }
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.AccountNumber, recipient.accountNumber)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.RoutingNumber, recipient.routingNumber)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.FirstName, recipient.firstName)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.LastName, recipient.lastName)
    selectTangemPayBankWithdrawResidency(TangemPayBankWithdrawField.Country, recipient.country, recipient.region)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.City, recipient.city)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.AddressLine1, recipient.addressLine1)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.PostalCode, recipient.postalCode)
    clickTangemPayBankWithdrawFormContinue()
}

fun BaseTestCase.fillTangemPayBankDetails(recipient: TangemPayBankRecipient) {
    step("Assert bank details step is displayed") {
        awaitSuccess {
            onTangemPayBankWithdrawFormScreen { field(TangemPayBankWithdrawField.BankName).assertIsDisplayed() }
        }
    }
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.BankName, recipient.bankName)
    selectTangemPayBankWithdrawResidency(TangemPayBankWithdrawField.BankRegion, recipient.bankRegion)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.BankCity, recipient.bankCity)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.BankAddressLine1, recipient.bankAddressLine1)
    enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.BankPostalCode, recipient.bankPostalCode)
    clickTangemPayBankWithdrawFormContinue()
}

@OptIn(ExperimentalTestApi::class)
fun BaseTestCase.sendTangemPayBankWithdrawal(accessCode: String) {
    step("Click on 'Send' button") {
        awaitSuccess { onTangemPayBankWithdrawPreviewScreen { sendButton.assertIsEnabled() } }
        onTangemPayBankWithdrawPreviewScreen { sendButton.performClick() }
    }
    step("Enter access code") {
        // Raw Compose on purpose: the dialog content waits for frames a 10 s Kakao retry would not advance.
        composeTestRule.waitUntilAtLeastOneExists(unlockDialogAccessCodeInputMatcher, WAIT_UNTIL_TIMEOUT)
        onHotWalletAccessCodeScreen { unlockDialogAccessCodeInput.performTextInput(accessCode) }
    }
    step("Dismiss keyboard") {
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }
}

private fun BaseTestCase.enterTangemPayBankWithdrawField(field: TangemPayBankWithdrawField, value: String) {
    step("Enter '$value' into '${field.name}' field") {
        onTangemPayBankWithdrawFormScreen {
            input(field).performScrollTo()
            input(field).performTextReplacement(value)
        }
    }
}

private fun BaseTestCase.selectTangemPayBankWithdrawResidency(
    field: TangemPayBankWithdrawField,
    vararg names: String,
) {
    step("Click on '${field.name}' field") {
        Espresso.closeSoftKeyboard()
        onTangemPayBankWithdrawFormScreen {
            field(field).performScrollTo()
            field(field).performClick()
        }
    }
    names.forEach { name ->
        step("Click on '$name'") {
            awaitSuccess { onTangemPayResidencySelectorScreen { row(name).assertIsDisplayed() } }
            onTangemPayResidencySelectorScreen { row(name).performClick() }
        }
    }
    step("Assert '${field.name}' field shows '${names.first()}'") {
        awaitSuccess { onTangemPayBankWithdrawFormScreen { valueOf(field, names.first()).assertIsDisplayed() } }
    }
}

private fun BaseTestCase.clickTangemPayBankWithdrawFormContinue() {
    step("Dismiss keyboard") {
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }
    step("Click on 'Continue' button") {
        awaitSuccess { onTangemPayBankWithdrawFormScreen { continueButton.assertIsEnabled() } }
        onTangemPayBankWithdrawFormScreen { continueButton.performClick() }
    }
}