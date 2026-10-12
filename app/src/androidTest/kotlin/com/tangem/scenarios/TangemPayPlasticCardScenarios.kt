package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.screens.tangempay.*
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step

private const val CUSTOMER_SCENARIO = "tangem_pay_plastic_customer"
private const val OFFERS_SCENARIO = "tangem_pay_plastic_offers"
private const val ORDER_SCENARIO = "tangem_pay_plastic_order"
private const val ACTIVATION_SCENARIO = "tangem_pay_plastic_activation"

private const val ELIGIBILITY_PAERA_CUSTOMER_STATE = "PaeraCustomer"
private const val COMPLETED_STATE = "Completed"

private const val CUSTOMER_ME_ENDPOINT = "/bff-v2/v1/customer/me"

private const val FIRST_NAME = "John"
private const val LAST_NAME = "Doe"
private const val REGION = "California"
private const val CITY = "Los Angeles"
private const val ADDRESS_LINE1 = "1 Main Street"
private const val POSTAL_CODE = "90001"
private const val PHONE_DIGITS = "2025550123"

private val TANGEM_PAY_EDITABLE_ORDER_DATA_FIELDS = listOf(
    TangemPayOrderDataField.NameOnCard,
    TangemPayOrderDataField.FirstName,
    TangemPayOrderDataField.LastName,
    TangemPayOrderDataField.Region,
    TangemPayOrderDataField.City,
    TangemPayOrderDataField.AddressLine1,
    TangemPayOrderDataField.AddressLine2,
    TangemPayOrderDataField.PostalCode,
    TangemPayOrderDataField.Phone,
)

fun setTangemPayPlasticScenarios(customer: String, offers: String, order: String? = null) {
    setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, ELIGIBILITY_PAERA_CUSTOMER_STATE)
    setWireMockScenarioState(CUSTOMER_SCENARIO, customer)
    setWireMockScenarioState(OFFERS_SCENARIO, offers)
    order?.let { setWireMockScenarioState(ORDER_SCENARIO, it) }
}

fun stageTangemPayPlasticCustomer(customer: String) {
    step("Switch WireMock scenario '$CUSTOMER_SCENARIO' to '$customer'") {
        setWireMockScenarioState(CUSTOMER_SCENARIO, customer)
    }
}

fun stageTangemPayPlasticOffers(offers: String) {
    step("Switch WireMock scenario '$OFFERS_SCENARIO' to '$offers'") {
        setWireMockScenarioState(OFFERS_SCENARIO, offers)
    }
}

fun completeTangemPayPlasticOrder(deliveredCustomer: String) {
    stageTangemPayPlasticCustomer(deliveredCustomer)
    step("Switch WireMock scenario '$ORDER_SCENARIO' to '$COMPLETED_STATE'") {
        setWireMockScenarioState(ORDER_SCENARIO, COMPLETED_STATE)
    }
}

fun completeTangemPayPlasticActivation(activatedCustomer: String) {
    stageTangemPayPlasticCustomer(activatedCustomer)
    step("Switch WireMock scenario '$ACTIVATION_SCENARIO' to '$COMPLETED_STATE'") {
        setWireMockScenarioState(ACTIVATION_SCENARIO, COMPLETED_STATE)
    }
}

fun BaseTestCase.assertTangemPayRequestSent(method: String, endpoint: String, countBefore: Int) {
    awaitSuccess {
        if (getWireMockRequestCount(method, endpoint) <= countBefore) {
            throw AssertionError("The $method $endpoint request should be sent")
        }
    }
}

fun BaseTestCase.refreshTangemPayPaymentAccount() {
    val countBefore = getWireMockRequestCount("GET", CUSTOMER_ME_ENDPOINT)
    step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
    step("Assert 'customer/me' request was sent") {
        assertTangemPayRequestSent("GET", CUSTOMER_ME_ENDPOINT, countBefore)
    }
}

fun BaseTestCase.openTangemPayOrderCardType() {
    step("Click on 'Add card' button") {
        awaitSuccess { onTangemPayMainScreen { addCardButton.assertIsDisplayed() } }
        onTangemPayMainScreen { addCardButton.performClick() }
    }
    step("Assert 'Order new card' screen is displayed") {
        awaitSuccess { onTangemPayOrderCardTypeScreen { title.assertIsDisplayed() } }
    }
}

fun BaseTestCase.selectTangemPayPlasticTab() {
    step("Click on 'Plastic' tab") {
        onTangemPayOrderCardTypeScreen { plasticTabItem.performClick() }
    }
    step("Assert 'Plastic' tab is selected") {
        awaitSuccess { onTangemPayOrderCardTypeScreen { plasticTabItem.assertIsSelected() } }
    }
}

fun BaseTestCase.openTangemPayPlasticOffer() {
    openTangemPayOrderCardType()
    selectTangemPayPlasticTab()
}

fun BaseTestCase.closeTangemPayOrderCardType() {
    step("Click on device 'Back' button") {
        device.uiDevice.pressBack()
    }
    step("Assert payment account balance is displayed") {
        awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
    }
}

fun BaseTestCase.assertTangemPayVirtualOffer(issueFee: String) {
    step("Assert 'Virtual' tab is selected") {
        awaitSuccess { onTangemPayOrderCardTypeScreen { virtualTabItem.assertIsSelected() } }
    }
    step("Assert 'Issue fee' is '$issueFee'") {
        awaitSuccess {
            onTangemPayOrderCardTypeScreen {
                issueFeeTitle.assertIsDisplayed()
                text(issueFee).assertIsDisplayed()
            }
        }
    }
    step("Assert 'Delivery time' is 'Instant'") {
        onTangemPayOrderCardTypeScreen {
            deliveryTimeTitle.assertIsDisplayed()
            instantDeliveryTime.assertIsDisplayed()
        }
    }
}

fun BaseTestCase.assertTangemPayPlasticOffer(country: String, deliveryFee: String, minDays: Int, maxDays: Int) {
    step("Assert 'Deliver to' is '$country' with the 'Country of residence' hint") {
        awaitSuccess {
            onTangemPayOrderCardTypeScreen {
                deliverToTitle.assertIsDisplayed()
                text(country).assertIsDisplayed()
                countryOfResidenceHint.assertIsDisplayed()
            }
        }
    }
    step("Assert 'Delivery fee' is '$deliveryFee'") {
        onTangemPayOrderCardTypeScreen {
            deliveryFeeTitle.assertIsDisplayed()
            text(deliveryFee).assertIsDisplayed()
        }
    }
    step("Assert 'Delivery time' is '$minDays–$maxDays' business days") {
        onTangemPayOrderCardTypeScreen {
            deliveryTimeTitle.assertIsDisplayed()
            deliveryEtaRange(minDays, maxDays).assertIsDisplayed()
        }
    }
}

fun BaseTestCase.assertTangemPayPlasticOfferUnavailable(country: String) {
    step("Assert 'Unavailable' badge is displayed next to '$country'") {
        awaitSuccess {
            onTangemPayOrderCardTypeScreen {
                unavailableBadge.assertIsDisplayed()
                text(country).assertIsDisplayed()
            }
        }
    }
    step("Assert 'Delivery fee' and 'Delivery time' have no data") {
        onTangemPayOrderCardTypeScreen {
            deliveryFeeTitle.assertIsDisplayed()
            deliveryTimeTitle.assertIsDisplayed()
            noDataValueAt(index = 0).assertIsDisplayed()
            noDataValueAt(index = 1).assertIsDisplayed()
        }
    }
    step("Assert 'Select' button is disabled") {
        onTangemPayOrderCardTypeScreen { selectButton.assertIsNotEnabled() }
    }
}

fun BaseTestCase.openTangemPayDeliveryAddressForm() {
    openTangemPayPlasticOffer()
    step("Click on 'Select' button") {
        awaitSuccess { onTangemPayOrderCardTypeScreen { selectButton.assertIsEnabled() } }
        onTangemPayOrderCardTypeScreen { selectButton.performClick() }
    }
    step("Assert delivery address form is displayed") {
        awaitSuccess { onTangemPayOrderCardDataScreen { subtitle.assertIsDisplayed() } }
    }
}

fun BaseTestCase.fillTangemPayDeliveryAddress() {
    val values = listOf(
        TangemPayOrderDataField.FirstName to FIRST_NAME,
        TangemPayOrderDataField.LastName to LAST_NAME,
        TangemPayOrderDataField.Region to REGION,
        TangemPayOrderDataField.City to CITY,
        TangemPayOrderDataField.AddressLine1 to ADDRESS_LINE1,
        TangemPayOrderDataField.PostalCode to POSTAL_CODE,
        TangemPayOrderDataField.Phone to PHONE_DIGITS,
    )
    values.forEach { (field, value) -> enterTangemPayDeliveryAddressValue(field, value) }
}

fun BaseTestCase.blurTangemPayDeliveryAddressFields() {
    TANGEM_PAY_EDITABLE_ORDER_DATA_FIELDS.forEach { field ->
        step("Focus '${field.name}' field") {
            onTangemPayOrderCardDataScreen { focus(field) }
        }
    }
    step("Click on keyboard 'Done' button") {
        onTangemPayOrderCardDataScreen { input(TangemPayOrderDataField.Phone).performImeAction() }
    }
}

fun BaseTestCase.assertTangemPayRequiredFieldErrors() {
    val requiredFields = TANGEM_PAY_EDITABLE_ORDER_DATA_FIELDS - TangemPayOrderDataField.AddressLine2
    requiredFields.forEach { field ->
        step("Assert 'Required field' error is displayed under '${field.name}' field") {
            onTangemPayOrderCardDataScreen {
                requiredError(field).performScrollTo()
                requiredError(field).assertIsDisplayed()
            }
        }
    }
    step("Assert 'Required field' error is not displayed under 'AddressLine2' field") {
        onTangemPayOrderCardDataScreen {
            requiredError(TangemPayOrderDataField.AddressLine2).assertDoesNotExist()
        }
    }
}

fun BaseTestCase.assertTangemPayLatinOnlyFieldErrors(nonLatinText: String) {
    val textFields = TANGEM_PAY_EDITABLE_ORDER_DATA_FIELDS - TangemPayOrderDataField.Phone
    textFields.forEach { field ->
        enterTangemPayDeliveryAddressValue(field, nonLatinText)
        step("Assert 'Use Latin characters only' error is displayed under '${field.name}' field") {
            awaitSuccess { onTangemPayOrderCardDataScreen { latinOnlyError(field).assertIsDisplayed() } }
        }
    }
}

fun BaseTestCase.enterTangemPayDeliveryAddressValue(field: TangemPayOrderDataField, value: String) {
    step("Enter '$value' into '${field.name}' field") {
        onTangemPayOrderCardDataScreen {
            input(field).performScrollTo()
            input(field).performTextReplacement(value)
        }
    }
}

fun BaseTestCase.clickTangemPayOrderCardButton() {
    step("Click on 'Order card' button") {
        awaitSuccess { onTangemPayOrderCardDataScreen { orderButton.assertIsEnabled() } }
        onTangemPayOrderCardDataScreen { orderButton.performClick() }
    }
}

fun BaseTestCase.orderTangemPayPlasticCard() {
    openTangemPayDeliveryAddressForm()
    fillTangemPayDeliveryAddress()
    clickTangemPayOrderCardButton()
    step("Assert 'Card ordered' screen is displayed") {
        awaitSuccess { onTangemPayOrderCardSuccessScreen { title.assertIsDisplayed() } }
    }
}

fun BaseTestCase.assertTangemPayOrderCardSuccessContent(deliveryEtaMaxDays: Int, email: String) {
    step("Assert 'Card ordered' title is displayed") {
        onTangemPayOrderCardSuccessScreen { title.assertIsDisplayed() }
    }
    step("Assert delivery time of up to $deliveryEtaMaxDays business days is displayed") {
        onTangemPayOrderCardSuccessScreen { deliveryEta(deliveryEtaMaxDays).assertIsDisplayed() }
    }
    step("Assert email note with '$email' is displayed") {
        onTangemPayOrderCardSuccessScreen { emailNote(email).assertIsDisplayed() }
    }
}

fun BaseTestCase.showTangemPayOrderedCard() {
    step("Click on 'Show card' button") {
        onTangemPayOrderCardSuccessScreen { showCardButton.performClick() }
    }
    step("Assert payment account balance is displayed") {
        awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
    }
}

fun BaseTestCase.assertTangemPayDeliveryBanner() {
    step("Assert 'Activate card' button is displayed on the delivery banner") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayMainScreen { deliveryBannerActivateButton.assertIsDisplayed() }
        }
    }
    step("Assert delivery banner title and description are displayed") {
        onTangemPayMainScreen {
            deliveryBannerTitle.assertIsDisplayed()
            deliveryBannerMessage.assertIsDisplayed()
        }
    }
}

fun BaseTestCase.openTangemPayActivationFromDeliveryBanner() {
    step("Click on 'Activate card' button on the delivery banner") {
        onTangemPayMainScreen { deliveryBannerActivateButton.performClick() }
    }
    step("Assert card activation screen is displayed") {
        awaitSuccess { onTangemPayCardActivationScreen { lastDigitsInput.assertIsDisplayed() } }
    }
}

fun BaseTestCase.openTangemPayActivationFromCardPage() {
    step("Click on 'Activate card' button") {
        onTangemPayCardPageScreen { deliveryActivateButton.performClick() }
    }
    step("Assert card activation screen is displayed") {
        awaitSuccess { onTangemPayCardActivationScreen { lastDigitsInput.assertIsDisplayed() } }
    }
}

fun BaseTestCase.assertTangemPayCardActivationScreen() {
    step("Assert 'Activate card' header is displayed") {
        onTangemPayCardActivationScreen { topBarHeader.assertIsDisplayed() }
    }
    step("Assert last digits input is displayed") {
        onTangemPayCardActivationScreen { lastDigitsInput.assertIsDisplayed() }
    }
}

fun BaseTestCase.assertTangemPayCardActivationInitialState() {
    step("Assert 'Activate card' header is displayed") {
        onTangemPayCardActivationScreen { topBarHeader.assertIsDisplayed() }
    }
    step("Assert 'Enter last 4 digits' hint is displayed") {
        onTangemPayCardActivationScreen {
            hint.assertTextEquals(getResourceString(CoreResR.string.tangempay_card_activation_description))
        }
    }
    step("Assert 'Continue' button is disabled") {
        onTangemPayCardActivationScreen { continueButton.assertIsNotEnabled() }
    }
}

// Typed on the device keyboard without a tap, so the digits land in the input only if it already holds the focus.
fun BaseTestCase.enterTangemPayCardLastDigits(lastDigits: String) {
    step("Type '$lastDigits' on the device keyboard") {
        device.uiDevice.executeShellCommand("input text $lastDigits")
    }
    step("Assert '$lastDigits' is entered into the focused last digits input") {
        awaitSuccess { onTangemPayCardActivationScreen { lastDigitsInput.assertTextEquals(lastDigits) } }
    }
    step("Assert 'Continue' button is enabled") {
        awaitSuccess { onTangemPayCardActivationScreen { continueButton.assertIsEnabled() } }
    }
}

fun BaseTestCase.activateTangemPayPlasticCard(lastDigits: String) {
    enterTangemPayCardLastDigits(lastDigits)
    step("Click on 'Continue' button") {
        onTangemPayCardActivationScreen { continueButton.performClick() }
    }
    step("Assert the card is being activated") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayCardPageScreen { activatingInProgressBlock.assertIsDisplayed() }
        }
    }
}

fun BaseTestCase.closeTangemPayCardActivation() {
    step("Click on 'Close' button") {
        onTangemPayCardActivationScreen { closeButton.performClick() }
    }
    step("Assert card activation screen is closed") {
        awaitSuccess { onTangemPayCardActivationScreen { lastDigitsInput.assertDoesNotExist() } }
    }
}

fun BaseTestCase.assertTangemPayPlasticDeliveryState(email: String) {
    step("Assert delivery description with '$email' is displayed") {
        awaitSuccess { onTangemPayCardPageScreen { deliveryDescription(email).assertIsDisplayed() } }
    }
    step("Assert 'Contact support' and 'Activate card' buttons are displayed") {
        onTangemPayCardPageScreen {
            deliveryContactSupportButton.assertIsDisplayed()
            deliveryActivateButton.assertIsDisplayed()
        }
    }
}

fun BaseTestCase.assertTangemPayPlasticCardActivated(lastDigits: String) {
    step("Assert card management of the activated card is displayed") {
        awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
            onTangemPayCardPageScreen { freezeCardRowActive.assertIsDisplayed() }
        }
    }
    step("Assert the card ending with '$lastDigits' is displayed") {
        onTangemPayCardPageScreen { cardNumberShortWith(lastDigits).assertIsDisplayed() }
    }
    step("Assert card activation is not in progress") {
        onTangemPayCardPageScreen { activatingInProgressBlock.assertDoesNotExist() }
    }
}

fun BaseTestCase.openTangemPayCardPageWithDigits(lastDigits: String) {
    step("Click on the card tile ending with '$lastDigits'") {
        awaitSuccess { onTangemPayMainScreen { cardTileWithDigits(lastDigits).assertIsDisplayed() } }
        onTangemPayMainScreen { cardTileWithDigits(lastDigits).performClick() }
    }
    step("Assert card page 'More' button is displayed") {
        awaitSuccess { onTangemPayCardPageScreen { moreButton.assertIsDisplayed() } }
    }
}

fun BaseTestCase.openTangemPayCardAwaitingActivation() {
    step("Click on the tile of the card awaiting activation") {
        awaitSuccess { onTangemPayMainScreen { cardAwaitingActivationTile.assertIsDisplayed() } }
        onTangemPayMainScreen { cardAwaitingActivationTile.performClick() }
    }
    step("Assert card page 'Activate card' button is displayed") {
        awaitSuccess { onTangemPayCardPageScreen { deliveryActivateButton.assertIsDisplayed() } }
    }
}

fun BaseTestCase.returnToTangemPayPaymentAccount() {
    step("Click on device 'Back' button") {
        device.uiDevice.pressBack()
    }
    step("Assert payment account balance is displayed") {
        awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
    }
}

fun BaseTestCase.openTangemPayReplacePlasticSheet() {
    step("Click on 'More' button") {
        onTangemPayCardPageScreen { moreButton.performClick() }
    }
    step("Click on 'Replace card' menu item") {
        awaitSuccess { onTangemPayCardPageScreen { replaceCardMenuItem.assertIsDisplayed() } }
        onTangemPayCardPageScreen { replaceCardMenuItem.performClick() }
    }
    step("Assert 'Replace plastic card?' bottom sheet is displayed") {
        awaitSuccess { onTangemPayReissuePlasticSheet { title.assertIsDisplayed() } }
    }
}

fun BaseTestCase.assertTangemPayReplacePlasticAffordable(deliveryFee: String) {
    assertTangemPayReplacePlasticDeliveryFee(deliveryFee)
    step("Assert 'Not enough money' badge is not displayed") {
        onTangemPayReissuePlasticSheet { notEnoughMoneyBadge.assertDoesNotExist() }
    }
    step("Assert 'Replace card' button is enabled") {
        onTangemPayReissuePlasticSheet { replaceButton.assertIsEnabled() }
    }
}

fun BaseTestCase.assertTangemPayReplacePlasticNotAffordable(deliveryFee: String) {
    assertTangemPayReplacePlasticDeliveryFee(deliveryFee)
    step("Assert 'Not enough money' badge is displayed") {
        onTangemPayReissuePlasticSheet { notEnoughMoneyBadge.assertIsDisplayed() }
    }
    step("Assert 'Replace card' button is disabled") {
        onTangemPayReissuePlasticSheet { replaceButton.assertIsNotEnabled() }
    }
}

private fun BaseTestCase.assertTangemPayReplacePlasticDeliveryFee(deliveryFee: String) {
    step("Assert 'Delivery fee' is '$deliveryFee'") {
        awaitSuccess {
            onTangemPayReissuePlasticSheet {
                deliveryFeeTitle.assertIsDisplayed()
                text(deliveryFee).assertIsDisplayed()
            }
        }
    }
}