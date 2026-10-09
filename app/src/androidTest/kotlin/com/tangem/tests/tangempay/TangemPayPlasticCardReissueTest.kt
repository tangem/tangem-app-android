package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayPlasticCardReissueTest : BaseTestCase() {

    private val plasticActiveState = "PlasticActive"
    private val plasticActiveEmptyBalanceState = "PlasticActiveEmptyBalance"
    private val plasticReissueDeliveredState = "PlasticReissueDelivered"
    private val plasticReissueActiveState = "PlasticReissueActive"
    private val paidDeliveryState = "PaidDelivery"

    private val country = "United States"
    private val email = "plastic.user@tangem.com"
    private val deliveryFee = "$4.99"
    private val deliveryEtaMaxDays = 4
    private val emptyBalance = "0.00"
    private val fundedBalance = "10.00"
    private val plasticCardLastDigits = "8181"
    private val reissuedCardLastDigits = "9191"
    private val validLastDigits = "4545"

    private val reissueOffersEndpoint = "/bff-v2/v1/product-instances/mock-product-id-plastic/offers"

    @AllureId("11979")
    @DisplayName("Tangem Pay: 'Replace plastic card?' sheet shows the reissue offer")
    @Test
    fun replacePlasticCardSheetShowsReissueOfferTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = plasticActiveState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open card management of the plastic card") {
                openTangemPayCardPageWithDigits(plasticCardLastDigits)
            }
            val offersBefore = getWireMockRequestCount("GET", reissueOffersEndpoint)
            step("Open 'Replace plastic card?' sheet") { openTangemPayReplacePlasticSheet() }
            step("Assert the plastic reissue offers request was sent") {
                assertTangemPayRequestSent("GET", reissueOffersEndpoint, offersBefore)
            }
            step("Assert the sheet title and description are displayed") {
                onTangemPayReissuePlasticSheet {
                    title.assertIsDisplayed()
                    description.assertIsDisplayed()
                }
            }
            step("Assert 'Deliver to' is '$country'") {
                awaitSuccess {
                    onTangemPayReissuePlasticSheet {
                        deliverToTitle.assertIsDisplayed()
                        text(country).assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Delivery time' is up to $deliveryEtaMaxDays business days") {
                onTangemPayReissuePlasticSheet {
                    deliveryTimeTitle.assertIsDisplayed()
                    deliveryTime(deliveryEtaMaxDays).assertIsDisplayed()
                }
            }
            step("Assert the '$deliveryFee' delivery fee is affordable") {
                assertTangemPayReplacePlasticAffordable(deliveryFee)
            }
            step("Assert 'Cancel' button is displayed") {
                onTangemPayReissuePlasticSheet { cancelButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("11957")
    @DisplayName("Tangem Pay: plastic card replacement is blocked while the balance is below the delivery fee")
    @Test
    fun replacePlasticCardBlockedBelowDeliveryFeeTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = plasticActiveEmptyBalanceState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Assert payment account balance contains '$emptyBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(emptyBalance, substring = true) }
                }
            }
            step("Open card management of the plastic card") {
                openTangemPayCardPageWithDigits(plasticCardLastDigits)
            }
            step("Open 'Replace plastic card?' sheet") { openTangemPayReplacePlasticSheet() }
            step("Assert the '$deliveryFee' delivery fee is not affordable") {
                assertTangemPayReplacePlasticNotAffordable(deliveryFee)
            }
            step("Click on 'Cancel' button") {
                onTangemPayReissuePlasticSheet { cancelButton.performClick() }
            }
            step("Assert 'Replace plastic card?' sheet is not displayed") {
                awaitSuccess { onTangemPayReissuePlasticSheet { title.assertIsNotDisplayed() } }
            }
            step("Return to the payment account") { returnToTangemPayPaymentAccount() }
            step("Top up the payment account above the delivery fee") {
                stageTangemPayPlasticCustomer(plasticActiveState)
            }
            step("Refresh the payment account") { refreshTangemPayPaymentAccount() }
            step("Assert payment account balance contains '$fundedBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(fundedBalance, substring = true) }
                }
            }
            step("Open card management of the plastic card") {
                openTangemPayCardPageWithDigits(plasticCardLastDigits)
            }
            step("Open 'Replace plastic card?' sheet") { openTangemPayReplacePlasticSheet() }
            step("Assert the '$deliveryFee' delivery fee is affordable") {
                assertTangemPayReplacePlasticAffordable(deliveryFee)
            }
        }
    }

    @AllureId("11980")
    @DisplayName("Tangem Pay: a plastic card goes from the replacement to the activated new card")
    @Test
    fun plasticCardFromReplacementToActivationTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = plasticActiveState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open card management of the plastic card") {
                openTangemPayCardPageWithDigits(plasticCardLastDigits)
            }
            step("Open 'Replace plastic card?' sheet") { openTangemPayReplacePlasticSheet() }
            step("Assert the '$deliveryFee' delivery fee is affordable") {
                assertTangemPayReplacePlasticAffordable(deliveryFee)
            }
            step("Click on 'Replace card' button") {
                onTangemPayReissuePlasticSheet { replaceButton.performClick() }
            }
            step("Assert delivery address form is displayed") {
                awaitSuccess { onTangemPayOrderCardDataScreen { subtitle.assertIsDisplayed() } }
            }
            step("Fill in the delivery address") { fillTangemPayDeliveryAddress() }
            step("Order the replacement card") { clickTangemPayOrderCardButton() }
            step("Assert 'Card ordered' screen content") {
                awaitSuccess { onTangemPayOrderCardSuccessScreen { title.assertIsDisplayed() } }
                assertTangemPayOrderCardSuccessContent(deliveryEtaMaxDays, email)
            }
            step("Click on 'Close' button") {
                onTangemPayOrderCardSuccessScreen { closeButton.performClick() }
            }
            step("Assert payment account balance is displayed") {
                awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
            }
            step("Complete the replacement order in WireMock") {
                completeTangemPayPlasticOrder(plasticReissueDeliveredState)
            }
            step("Assert the delivery banner") { assertTangemPayDeliveryBanner() }
            step("Open card activation from the delivery banner") { openTangemPayActivationFromDeliveryBanner() }
            step("Activate the card with '$validLastDigits'") { activateTangemPayPlasticCard(validLastDigits) }
            step("Complete the activation order in WireMock") {
                completeTangemPayPlasticActivation(plasticReissueActiveState)
            }
            step("Assert the replacement plastic card is activated") {
                assertTangemPayPlasticCardActivated(reissuedCardLastDigits)
            }
        }
    }
}