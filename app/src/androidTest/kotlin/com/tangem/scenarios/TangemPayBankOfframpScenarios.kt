package com.tangem.scenarios

import android.text.format.DateFormat
import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankOfframpOrder
import com.tangem.common.constants.TangemPayBankRecipient
import com.tangem.common.constants.TangemPayBankRoute
import com.tangem.common.constants.TangemPayBankRoutes
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.isDisplayedSafely
import com.tangem.common.extensions.tapBackButton
import com.tangem.common.utils.findWireMockRequestBodies
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.screens.tangempay.TangemPayBankWithdrawField
import com.tangem.screens.tangempay.onTangemPayBankRecipientsScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawAmountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawChoiceScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawFormScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawPreviewScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawSuccessScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.screens.tangempay.onTangemPayResidencySelectorScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import org.json.JSONObject
import org.junit.Assert.assertEquals
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun BaseTestCase.setTangemPayBankOfframpScenarios() {
    val channelsBankOfframpState = "BankOfframp"

    setTangemPayMultiNetworkScenarios()
    step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBankOfframpState'") {
        setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBankOfframpState)
    }
}

fun BaseTestCase.setTangemPayBankOfframpTransferScenarios() {
    val balanceScenario = "tangem_pay_balance_update"
    val balanceBankOfframpState = "MultiNetworkBankOfframp"

    setTangemPayBankOfframpScenarios()
    step("Set WireMock scenario '$balanceScenario' to '$balanceBankOfframpState'") {
        setWireMockScenarioState(balanceScenario, balanceBankOfframpState)
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

fun BaseTestCase.openTangemPayRecentRecipient(recipient: TangemPayBankRecipient) {
    step("Assert recent recipient '${recipient.shortName}' is displayed") {
        assertTangemPayRecentRecipientDisplayed(recipient)
    }
    step("Click on recent recipient '${recipient.shortName}'") {
        onTangemPayWithdrawHubScreen { recentRecipientWithName(recipient.shortName).performClick() }
    }
    step("Assert 'Amount' screen for '${recipient.displayName}' is displayed") {
        awaitSuccess { onTangemPayBankWithdrawAmountScreen { headerTitle(recipient.displayName).assertIsDisplayed() } }
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
    // The amount field reopens the keyboard under the sheet, and Back then closes the keyboard instead.
    step("Close 'Choose network' sheet") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            Espresso.closeSoftKeyboard()
            onTangemPayBankWithdrawAmountScreen {
                if (tokenSheetTitle.isDisplayedSafely()) {
                    device.uiDevice.pressBack()
                    composeTestRule.waitForIdle()
                }
                tokenSheetTitle.assertDoesNotExist()
            }
        }
    }
    step("Click on 'Back' button") { tapBackButton() }
    step("Assert 'Withdraw' hub screen is displayed") {
        awaitSuccess { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
    }
    returnToPaymentAccountFromWithdrawHub()
}

fun BaseTestCase.assertTangemPayBankWithdrawDefaults() {
    step("Assert 'USDC' on 'Polygon' is the default source") {
        awaitSuccess { onTangemPayBankWithdrawAmountScreen { sourceRowWith("USDC", "Polygon").assertIsDisplayed() } }
    }
    step("Assert '${TangemPayBankRoutes.ACH.title}' is the default route") {
        onTangemPayBankWithdrawAmountScreen { routeRow(TangemPayBankRoutes.ACH).assertIsDisplayed() }
    }
}

fun BaseTestCase.openTangemPayBankWithdrawRouteSelector() {
    step("Dismiss keyboard") {
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }
    step("Click on the route row") {
        onTangemPayBankWithdrawAmountScreen { routeSelector.performClick() }
    }
    step("Assert 'Select route' sheet is displayed") {
        awaitSuccess { onTangemPayBankWithdrawAmountScreen { routeSheetTitle.assertIsDisplayed() } }
    }
}

fun BaseTestCase.chooseTangemPayBankWithdrawRoute(route: TangemPayBankRoute) {
    openTangemPayBankWithdrawRouteSelector()
    step("Click on '${route.title}' route") {
        onTangemPayBankWithdrawAmountScreen { routeOption(route).performClick() }
    }
    step("Assert '${route.title}' route is selected") {
        awaitSuccess { onTangemPayBankWithdrawAmountScreen { routeRow(route).assertIsDisplayed() } }
    }
}

fun BaseTestCase.chooseTangemPayBankWithdrawSource(symbol: String, network: String) {
    openTangemPayBankWithdrawSourceSelector()
    // The amount field reopens the keyboard under the sheet, and it swallows taps on the lower rows.
    step("Click on '$symbol' on '$network' until it is the source") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            Espresso.closeSoftKeyboard()
            onTangemPayBankWithdrawAmountScreen {
                if (tokenRow(symbol, network).isDisplayedSafely()) {
                    tokenRow(symbol, network).performClick()
                    composeTestRule.waitForIdle()
                }
                sourceRowWith(symbol, network).assertIsDisplayed()
            }
        }
    }
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

fun BaseTestCase.assertTangemPayBankWithdrawChoiceStepOpened(sectionTitle: String) {
    step("Assert '$sectionTitle' step is displayed") {
        awaitSuccess { onTangemPayBankWithdrawChoiceScreen { sectionTitle(sectionTitle).assertIsDisplayed() } }
    }
    step("Assert 'Continue' button is disabled") {
        onTangemPayBankWithdrawChoiceScreen { continueButton.assertIsNotEnabled() }
    }
}

fun BaseTestCase.chooseTangemPayBankWithdrawOption(sectionTitle: String, option: String) {
    step("Assert '$sectionTitle' step is displayed") {
        awaitSuccess { onTangemPayBankWithdrawChoiceScreen { sectionTitle(sectionTitle).assertIsDisplayed() } }
    }
    selectTangemPayBankWithdrawOption(option)
}

fun BaseTestCase.chooseTangemPayBankWithdrawAccountTypes(transferType: String?, accountType: String) {
    val transferTypeTitle = getResourceString(CoreResR.string.tangempay_withdraw_bank_transfer_type_title)
    val accountTypeTitle = getResourceString(CoreResR.string.tangempay_withdraw_bank_account_type_title)

    if (transferType != null) {
        assertTangemPayBankWithdrawChoiceStepOpened(transferTypeTitle)
        selectTangemPayBankWithdrawOption(transferType)
    }
    assertTangemPayBankWithdrawChoiceStepOpened(accountTypeTitle)
    selectTangemPayBankWithdrawOption(accountType)
}

private fun BaseTestCase.selectTangemPayBankWithdrawOption(option: String) {
    step("Click on '$option' option") {
        onTangemPayBankWithdrawChoiceScreen { option(option).performClick() }
    }
    step("Click on 'Continue' button") {
        awaitSuccess { onTangemPayBankWithdrawChoiceScreen { continueButton.assertIsEnabled() } }
        onTangemPayBankWithdrawChoiceScreen { continueButton.performClick() }
    }
}

fun BaseTestCase.fillTangemPayNewBankRecipient(
    transferType: String?,
    accountType: String,
    recipient: TangemPayBankRecipient,
) {
    chooseTangemPayBankWithdrawAccountTypes(transferType, accountType)
    fillTangemPayBankRecipientDetails(recipient)
    fillTangemPayBankDetails(recipient)
    step("Assert 'Confirm transfer' screen is displayed") {
        awaitSuccess { onTangemPayBankWithdrawPreviewScreen { title.assertIsDisplayed() } }
    }
}

fun BaseTestCase.openTangemPayBankRecipientDetailsStep(transferType: String?, accountType: String) {
    val amount = "5"

    enterTangemPayBankWithdrawAmount(amount)
    step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
    chooseTangemPayBankWithdrawAccountTypes(transferType, accountType)
    step("Assert recipient details step is displayed") {
        awaitSuccess {
            onTangemPayBankWithdrawFormScreen { field(TangemPayBankWithdrawField.AccountNumber).assertIsDisplayed() }
        }
    }
}

fun BaseTestCase.closeTangemPayBankWithdrawForm() {
    step("Click on 'Close' button") {
        onTangemPayBankWithdrawFormScreen { closeButton.performClick() }
    }
    step("Assert payment account balance is displayed") {
        awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
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
    val businessName = recipient.businessName
    if (businessName != null) {
        enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.BusinessName, businessName)
    } else {
        enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.FirstName, recipient.firstName)
        enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.LastName, recipient.lastName)
    }
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

fun BaseTestCase.sendTangemPayBankWithdrawal(accessCode: String) {
    clickTangemPayBankWithdrawSend()
    enterHotWalletUnlockAccessCode(accessCode)
}

fun BaseTestCase.clickTangemPayBankWithdrawSend() {
    step("Click on 'Send' button") {
        awaitSuccess { onTangemPayBankWithdrawPreviewScreen { sendButton.assertIsEnabled() } }
        onTangemPayBankWithdrawPreviewScreen { sendButton.performClick() }
    }
}

fun BaseTestCase.assertTangemPayBankWithdrawSendingScreen(amount: String) {
    val sendingTitle = getResourceString(CoreResR.string.tangempay_withdraw_bank_success_title, "$$amount")
    awaitSuccess { onTangemPayBankWithdrawSuccessScreen { title(sendingTitle).assertIsDisplayed() } }
}

fun BaseTestCase.assertTangemPayBankWithdrawSendingDateToday() {
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMM d, yyyy")
    val today = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
    step("Assert date '$today' is displayed") {
        onTangemPayBankWithdrawSuccessScreen { subtitle(today).assertIsDisplayed() }
    }
}

fun assertTangemPayRequestSentOnce(method: String, endpoint: String, countBefore: Int) {
    assertEquals(
        "The $method $endpoint request should be sent once",
        countBefore + 1,
        getWireMockRequestCount(method, endpoint),
    )
}

fun BaseTestCase.closeTangemPayBankWithdrawSuccess() {
    step("Click on 'Close' button") {
        onTangemPayBankWithdrawSuccessScreen { closeButton.performClick() }
    }
    step("Assert payment account balance is displayed") {
        awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
    }
}

fun BaseTestCase.assertTangemPayBankWithdrawFieldError(
    field: TangemPayBankWithdrawField,
    value: String,
    blurField: TangemPayBankWithdrawField,
    error: String,
) {
    enterTangemPayBankWithdrawField(field, value)
    step("Move focus to '${blurField.name}' field") {
        onTangemPayBankWithdrawFormScreen { focus(blurField) }
    }
    step("Assert '$error' is displayed under '${field.name}' field") {
        awaitSuccess { onTangemPayBankWithdrawFormScreen { fieldError(field, error).assertIsDisplayed() } }
    }
}

fun BaseTestCase.assertTangemPayBankWithdrawFieldValid(
    field: TangemPayBankWithdrawField,
    value: String,
    blurField: TangemPayBankWithdrawField,
    error: String,
) {
    enterTangemPayBankWithdrawField(field, value)
    step("Move focus to '${blurField.name}' field") {
        onTangemPayBankWithdrawFormScreen { focus(blurField) }
    }
    step("Assert '$error' is not displayed under '${field.name}' field") {
        onTangemPayBankWithdrawFormScreen {
            field(field).assertIsDisplayed()
            fieldError(field, error).assertDoesNotExist()
        }
    }
}

fun countTangemPayBankOfframpOrders(order: TangemPayBankOfframpOrder): Int =
    findWireMockRequestBodies(method = "POST", urlPath = TANGEM_PAY_ORDER_ENDPOINT)
        .count { body -> body.toTangemPayBankOfframpOrder() == order }

fun BaseTestCase.assertTangemPayBankOfframpOrderSent(order: TangemPayBankOfframpOrder, countBefore: Int) {
    awaitSuccess {
        if (countTangemPayBankOfframpOrders(order) <= countBefore) {
            throw AssertionError("The bank off-ramp order $order should be sent")
        }
    }
}

fun BaseTestCase.enterTangemPayBankWithdrawField(field: TangemPayBankWithdrawField, value: String) {
    step("Enter '$value' into '${field.name}' field") {
        onTangemPayBankWithdrawFormScreen {
            input(field).performScrollTo()
            input(field).performTextReplacement(value)
        }
    }
}

fun BaseTestCase.selectTangemPayBankWithdrawResidency(
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

fun BaseTestCase.clickTangemPayBankWithdrawFormContinue() {
    step("Dismiss keyboard") {
        Espresso.closeSoftKeyboard()
        waitForIdle()
    }
    step("Click on 'Continue' button") {
        awaitSuccess { onTangemPayBankWithdrawFormScreen { continueButton.assertIsEnabled() } }
        onTangemPayBankWithdrawFormScreen { continueButton.performClick() }
    }
}

private fun String.toTangemPayBankOfframpOrder(): TangemPayBankOfframpOrder? = runCatching {
    val data = JSONObject(this).getJSONObject("data")
    if (data.optString("type") != BANK_OFFRAMP_ORDER_TYPE) return@runCatching null
    val details = data.optJSONObject("bank_account")?.optJSONObject("details")
    TangemPayBankOfframpOrder(
        amountInCents = data.getLong("amount_in_cents"),
        chainId = data.getInt("chain_id"),
        rail = details?.optStringOrNull("rail"),
        bankAccountType = details?.optStringOrNull("bank_account_type"),
        beneficiaryType = details?.optJSONObject("beneficiary")?.optStringOrNull("type"),
        isThirdParty = details?.takeIf { it.has("third_party") }?.getBoolean("third_party"),
        accountNumber = details?.optStringOrNull("account_number"),
        bankName = details?.optStringOrNull("bank_name"),
        bankAccountId = data.optStringOrNull("bank_account_id"),
    )
}.getOrNull()

private fun JSONObject.optStringOrNull(key: String): String? = if (has(key)) getString(key) else null

private const val TANGEM_PAY_ORDER_ENDPOINT = "/bff-v2/v1/order"
private const val BANK_OFFRAMP_ORDER_TYPE = "BANK_OFFRAMP_RAIN"