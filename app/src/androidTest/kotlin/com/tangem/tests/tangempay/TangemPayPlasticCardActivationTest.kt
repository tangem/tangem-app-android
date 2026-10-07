package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayPlasticCardActivationTest : BaseTestCase() {

    private val noPlasticState = "NoPlastic"
    private val plasticDeliveredState = "PlasticDelivered"
    private val plasticDeliveredBasicState = "PlasticDeliveredBasic"
    private val plasticActiveState = "PlasticActive"
    private val paidDeliveryState = "PaidDelivery"

    private val email = "plastic.user@tangem.com"
    private val deliveryEtaMaxDays = 4
    private val wrongLastDigits = "1234"
    private val validLastDigits = "4545"
    private val focusProbeDigits = "1111"
    private val plasticCardLastDigits = "8181"

    private val placeOrderEndpoint = "/bff-v2/v1/order"

    private val activatingHint = getResourceString(CoreResR.string.tangempay_card_activation_in_progress)
    private val invalidDigitsHint = getResourceString(CoreResR.string.tangempay_card_activation_error)

    @AllureId("11972")
    @DisplayName("Tangem Pay: a delivered plastic card shows the delivery state and both activation entries")
    @Test
    fun deliveredPlasticCardShowsDeliveryStateTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = noPlasticState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Order the plastic card") { orderTangemPayPlasticCard() }
            step("Show the ordered card") { showTangemPayOrderedCard() }
            step("Complete the plastic order in WireMock") { completeTangemPayPlasticOrder(plasticDeliveredState) }
            step("Assert the delivery banner") { assertTangemPayDeliveryBanner() }
            step("Open card activation from the delivery banner") { openTangemPayActivationFromDeliveryBanner() }
            step("Assert card activation screen is displayed") { assertTangemPayCardActivationScreen() }
            step("Close card activation") { closeTangemPayCardActivation() }
            step("Assert the plastic card delivery state") { assertTangemPayPlasticDeliveryState(email) }
            step("Return to the payment account") { returnToTangemPayPaymentAccount() }
            step("Open card management of the plastic card") { openTangemPayCardAwaitingActivation() }
            step("Assert the plastic card delivery state") { assertTangemPayPlasticDeliveryState(email) }
            step("Open card activation from card management") { openTangemPayActivationFromCardPage() }
            step("Assert card activation screen is displayed") { assertTangemPayCardActivationScreen() }
        }
    }

    @AllureId("11974")
    @DisplayName("Tangem Pay: wrong last digits fail the plastic card activation")
    @Test
    fun wrongLastDigitsFailActivationTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = plasticDeliveredState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert the delivery banner") { assertTangemPayDeliveryBanner() }
            step("Open card activation from the delivery banner") { openTangemPayActivationFromDeliveryBanner() }
            step("Assert card activation screen initial state") { assertTangemPayCardActivationInitialState() }
            step("Enter wrong last digits") { enterTangemPayCardLastDigits(wrongLastDigits) }
            val ordersBefore = getWireMockRequestCount("POST", placeOrderEndpoint)
            step("Click on 'Continue' button") {
                onTangemPayCardActivationScreen { continueButton.performClick() }
            }
            step("Assert '$activatingHint' hint is displayed") {
                awaitSuccess { onTangemPayCardActivationScreen { hint.assertTextEquals(activatingHint) } }
            }
            step("Assert the activation order request was sent") {
                assertTangemPayRequestSent("POST", placeOrderEndpoint, ordersBefore)
            }
            step("Assert '$invalidDigitsHint' hint is displayed") {
                awaitSuccess { onTangemPayCardActivationScreen { hint.assertTextEquals(invalidDigitsHint) } }
            }
        }
    }

    @AllureId("11976")
    @DisplayName("Tangem Pay: the plastic card activation screen opens on the Plus and Basic tariffs")
    @Test
    fun activationScreenOnPlusAndBasicTariffsTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = plasticDeliveredState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert the delivery banner") { assertTangemPayDeliveryBanner() }
            step("Open card activation from the delivery banner") { openTangemPayActivationFromDeliveryBanner() }
            step("Assert card activation screen initial state") { assertTangemPayCardActivationInitialState() }
            step("Assert the last digits input is focused") { enterTangemPayCardLastDigits(focusProbeDigits) }
            step("Close card activation") { closeTangemPayCardActivation() }
            step("Return to the payment account") { returnToTangemPayPaymentAccount() }
            step("Switch the customer to the Basic tariff") {
                stageTangemPayPlasticCustomer(plasticDeliveredBasicState)
            }
            step("Refresh the payment account") { refreshTangemPayPaymentAccount() }
            step("Open card management of the plastic card") { openTangemPayCardAwaitingActivation() }
            step("Assert the plastic card delivery state") { assertTangemPayPlasticDeliveryState(email) }
            step("Open card activation from card management") { openTangemPayActivationFromCardPage() }
            step("Assert card activation screen initial state") { assertTangemPayCardActivationInitialState() }
            step("Assert the last digits input is focused") { enterTangemPayCardLastDigits(focusProbeDigits) }
        }
    }

    @AllureId("11894")
    @DisplayName("Tangem Pay: a plastic card goes from the order to the activated card")
    @Test
    fun plasticCardFromOrderToActivationTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = noPlasticState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Order the plastic card") { orderTangemPayPlasticCard() }
            step("Assert 'Card ordered' screen content") {
                assertTangemPayOrderCardSuccessContent(deliveryEtaMaxDays, email)
            }
            step("Show the ordered card") { showTangemPayOrderedCard() }
            step("Complete the plastic order in WireMock") { completeTangemPayPlasticOrder(plasticDeliveredState) }
            step("Assert the delivery banner") { assertTangemPayDeliveryBanner() }
            step("Open card activation from the delivery banner") { openTangemPayActivationFromDeliveryBanner() }
            step("Activate the card with '$validLastDigits'") { activateTangemPayPlasticCard(validLastDigits) }
            step("Complete the activation order in WireMock") {
                completeTangemPayPlasticActivation(plasticActiveState)
            }
            step("Assert the plastic card is activated") {
                assertTangemPayPlasticCardActivated(plasticCardLastDigits)
            }
        }
    }
}