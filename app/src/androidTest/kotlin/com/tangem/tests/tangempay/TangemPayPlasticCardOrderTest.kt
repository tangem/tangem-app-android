package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.assertTextContainsSafe
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
class TangemPayPlasticCardOrderTest : BaseTestCase() {

    private val noPlasticState = "NoPlastic"
    private val plasticActiveEmptyBalanceState = "PlasticActiveEmptyBalance"
    private val plasticDeliveredState = "PlasticDelivered"
    private val plasticDeliveredBasicState = "PlasticDeliveredBasic"
    private val plasticActiveState = "PlasticActive"
    private val firstDeliveryFreeState = "FirstDeliveryFree"
    private val paidDeliveryState = "PaidDelivery"
    private val virtualOnlyState = "VirtualOnly"
    private val plasticIncompleteState = "PlasticIncomplete"
    private val orderErrorState = "OrderError"

    private val country = "United States"
    private val email = "plastic.user@tangem.com"
    private val phoneMaskHint = "+1 ___-___-____"
    private val virtualIssueFee = "$1.25"
    private val deliveryFee = "$4.99"
    private val deliveryEtaMinDays = 2
    private val deliveryEtaMaxDays = 4
    private val emptyBalance = "0.00"
    private val fundedBalance = "10.00"
    private val plasticCardLastDigits = "8181"
    private val virtualEmbossName = "HOT WALLET USER"
    private val plasticEmbossName = "PLASTIC CARD USER"
    private val nonLatinText = "Москва"

    private val offersEndpoint = "/bff-v2/v1/customer/offers"
    private val placeOrderEndpoint = "/bff-v2/v1/order"
    private val plasticOrderStatusEndpoint = "/bff-v2/v1/order/mock-plastic-order-001"

    private val betaLabel = getResourceString(CoreResR.string.tangempay_order_type_beta)

    @AllureId("11978")
    @DisplayName("Tangem Pay: the plastic card offers screen follows the Plus and Basic tariffs")
    @Test
    fun plasticCardOffersFollowTariffTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = plasticDeliveredState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open 'Order new card' screen") { openTangemPayOrderCardType() }
            step("Assert the Plus background is displayed") {
                awaitSuccess { onTangemPayOrderCardTypeScreen { plusBackground.assertIsDisplayed() } }
            }
            step("Assert the virtual offer with the '$virtualIssueFee' issue fee") {
                assertTangemPayVirtualOffer(virtualIssueFee)
            }
            step("Assert 'Select' button is enabled") {
                onTangemPayOrderCardTypeScreen { selectButton.assertIsEnabled() }
            }
            step("Assert 'Plastic' tab is labeled '$betaLabel'") {
                onTangemPayOrderCardTypeScreen { plasticTabItem.assertTextContains(betaLabel) }
            }
            step("Select 'Plastic' tab") { selectTangemPayPlasticTab() }
            step("Assert the Plus background is displayed") {
                onTangemPayOrderCardTypeScreen { plusBackground.assertIsDisplayed() }
            }
            step("Assert the plastic offer with the '$deliveryFee' delivery fee") {
                assertTangemPayPlasticOffer(country, deliveryFee, deliveryEtaMinDays, deliveryEtaMaxDays)
            }
            step("Assert 'Select' button is enabled") {
                onTangemPayOrderCardTypeScreen { selectButton.assertIsEnabled() }
            }
            step("Close 'Order new card' screen") { closeTangemPayOrderCardType() }
            step("Switch the customer to the Basic tariff") {
                stageTangemPayPlasticCustomer(plasticDeliveredBasicState)
            }
            step("Refresh the payment account") { refreshTangemPayPaymentAccount() }
            step("Open 'Order new card' screen") { openTangemPayOrderCardType() }
            step("Assert the Basic background is displayed") {
                awaitSuccess { onTangemPayOrderCardTypeScreen { basicBackground.assertIsDisplayed() } }
            }
            step("Assert the virtual offer with the '$virtualIssueFee' issue fee") {
                assertTangemPayVirtualOffer(virtualIssueFee)
            }
            step("Select 'Plastic' tab") { selectTangemPayPlasticTab() }
            step("Assert the Basic background is displayed") {
                onTangemPayOrderCardTypeScreen { basicBackground.assertIsDisplayed() }
            }
            step("Assert the plastic offer with the '$deliveryFee' delivery fee") {
                assertTangemPayPlasticOffer(country, deliveryFee, deliveryEtaMinDays, deliveryEtaMaxDays)
            }
            step("Assert 'Select' button is enabled") {
                onTangemPayOrderCardTypeScreen { selectButton.assertIsEnabled() }
            }
        }
    }

    @AllureId("11955")
    @DisplayName("Tangem Pay: the first plastic delivery on Plus is free until a plastic card exists")
    @Test
    fun plusFirstPlasticDeliveryIsFreeTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = noPlasticState, offers = firstDeliveryFreeState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the plastic offer") { openTangemPayPlasticOffer() }
            step("Assert 'First delivery for free' badge is displayed") {
                awaitSuccess { onTangemPayOrderCardTypeScreen { firstDeliveryFreeBadge.assertIsDisplayed() } }
            }
            step("Assert 'Select' button is enabled") {
                onTangemPayOrderCardTypeScreen { selectButton.assertIsEnabled() }
            }
            step("Close 'Order new card' screen") { closeTangemPayOrderCardType() }
            step("Switch the customer to an activated plastic card") {
                stageTangemPayPlasticCustomer(plasticActiveState)
            }
            step("Switch the offers to a paid delivery") { stageTangemPayPlasticOffers(paidDeliveryState) }
            step("Refresh the payment account") { refreshTangemPayPaymentAccount() }
            step("Assert the plastic card ending with '$plasticCardLastDigits' is displayed") {
                awaitSuccess {
                    onTangemPayMainScreen { cardTileWithDigits(plasticCardLastDigits).assertIsDisplayed() }
                }
            }
            step("Open the plastic offer") { openTangemPayPlasticOffer() }
            step("Assert '$deliveryFee' delivery fee is displayed") {
                awaitSuccess { onTangemPayOrderCardTypeScreen { text(deliveryFee).assertIsDisplayed() } }
            }
            step("Assert 'First delivery for free' badge is not displayed") {
                onTangemPayOrderCardTypeScreen { firstDeliveryFreeBadge.assertDoesNotExist() }
            }
            step("Assert 'Select' button is enabled") {
                onTangemPayOrderCardTypeScreen { selectButton.assertIsEnabled() }
            }
        }
    }

    @AllureId("11956")
    @DisplayName("Tangem Pay: plastic card order is blocked while the balance is below the delivery fee")
    @Test
    fun plasticOfferBlockedBelowDeliveryFeeTest() {
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
            step("Open the plastic offer") { openTangemPayPlasticOffer() }
            step("Assert 'Not enough money' badge and '$deliveryFee' delivery fee are displayed") {
                awaitSuccess {
                    onTangemPayOrderCardTypeScreen {
                        notEnoughMoneyBadge.assertIsDisplayed()
                        text(deliveryFee).assertIsDisplayed()
                    }
                }
            }
            step("Assert 'Select' button is disabled") {
                onTangemPayOrderCardTypeScreen { selectButton.assertIsNotEnabled() }
            }
            step("Close 'Order new card' screen") { closeTangemPayOrderCardType() }
            step("Top up the payment account above the delivery fee") {
                stageTangemPayPlasticCustomer(plasticActiveState)
            }
            step("Refresh the payment account") { refreshTangemPayPaymentAccount() }
            step("Assert payment account balance contains '$fundedBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(fundedBalance, substring = true) }
                }
            }
            step("Open the plastic offer") { openTangemPayPlasticOffer() }
            step("Assert '$deliveryFee' delivery fee is displayed") {
                awaitSuccess { onTangemPayOrderCardTypeScreen { text(deliveryFee).assertIsDisplayed() } }
            }
            step("Assert 'Not enough money' badge is not displayed") {
                onTangemPayOrderCardTypeScreen { notEnoughMoneyBadge.assertDoesNotExist() }
            }
            step("Assert 'Select' button is enabled") {
                onTangemPayOrderCardTypeScreen { selectButton.assertIsEnabled() }
            }
        }
    }

    @AllureId("11959")
    @DisplayName("Tangem Pay: plastic card is unavailable without a complete plastic offer")
    @Test
    fun plasticOfferUnavailableWithoutCompleteOfferTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = noPlasticState, offers = virtualOnlyState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the plastic offer") { openTangemPayPlasticOffer() }
            step("Assert the plastic offer is unavailable") { assertTangemPayPlasticOfferUnavailable(country) }
            step("Close 'Order new card' screen") { closeTangemPayOrderCardType() }
            step("Switch the offers to an incomplete plastic offer") {
                stageTangemPayPlasticOffers(plasticIncompleteState)
            }
            step("Reopen Tangem Pay from the Main screen") { reopenTangemPayFromMain() }
            val offersBefore = getWireMockRequestCount("GET", offersEndpoint)
            step("Open the plastic offer") { openTangemPayPlasticOffer() }
            step("Assert the customer offers request was sent") {
                assertTangemPayRequestSent("GET", offersEndpoint, offersBefore)
            }
            step("Assert the plastic offer is unavailable") { assertTangemPayPlasticOfferUnavailable(country) }
        }
    }

    @AllureId("11898")
    @DisplayName("Tangem Pay: the delivery address form validates required and Latin-only fields")
    @Test
    fun deliveryAddressFormValidationTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = noPlasticState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the delivery address form") { openTangemPayDeliveryAddressForm() }
            step("Clear 'Name on card' field") {
                onTangemPayOrderCardDataScreen {
                    input(TangemPayOrderDataField.NameOnCard).performTextClearance()
                }
            }
            step("Move the focus out of every field") { blurTangemPayDeliveryAddressFields() }
            step("Assert 'Required field' errors") { assertTangemPayRequiredFieldErrors() }
            step("Enter non-Latin text into every text field") {
                assertTangemPayLatinOnlyFieldErrors(nonLatinText)
            }
        }
    }

    @AllureId("11954")
    @DisplayName("Tangem Pay: the delivery address form is prefilled from the customer profile")
    @Test
    fun deliveryAddressFormPrefilledFromProfileTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = noPlasticState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the delivery address form") { openTangemPayDeliveryAddressForm() }
            step("Assert 'Name on card' is '$virtualEmbossName'") {
                onTangemPayOrderCardDataScreen {
                    valueOf(TangemPayOrderDataField.NameOnCard, virtualEmbossName).assertIsDisplayed()
                }
            }
            step("Assert disabled 'Country' is '$country'") {
                onTangemPayOrderCardDataScreen {
                    valueOf(TangemPayOrderDataField.Country, country).assertIsDisplayed()
                    valueOf(TangemPayOrderDataField.Country, country).assertIsNotEnabled()
                }
            }
            step("Assert disabled 'Email' is '$email'") {
                onTangemPayOrderCardDataScreen {
                    valueOf(TangemPayOrderDataField.Email, email).assertIsDisplayed()
                    valueOf(TangemPayOrderDataField.Email, email).assertIsNotEnabled()
                }
            }
            step("Assert 'Phone' shows the '$phoneMaskHint' mask") {
                onTangemPayOrderCardDataScreen {
                    input(TangemPayOrderDataField.Phone).performScrollTo()
                    input(TangemPayOrderDataField.Phone).assertTextEquals(phoneMaskHint)
                }
            }
        }
    }

    @AllureId("11897")
    @DisplayName("Tangem Pay: a follow-up plastic order prefills the name of the existing plastic card")
    @Test
    fun followUpPlasticOrderPrefillsPlasticEmbossNameTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = plasticActiveState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the delivery address form") { openTangemPayDeliveryAddressForm() }
            step("Assert 'Name on card' is '$plasticEmbossName'") {
                onTangemPayOrderCardDataScreen {
                    valueOf(TangemPayOrderDataField.NameOnCard, plasticEmbossName).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("11971")
    @DisplayName("Tangem Pay: a failed plastic card order shows the retry sheet")
    @Test
    fun plasticOrderFailureShowsRetrySheetTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(
                    customer = noPlasticState,
                    offers = paidDeliveryState,
                    order = orderErrorState,
                )
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            step("Open the delivery address form") { openTangemPayDeliveryAddressForm() }
            step("Fill in the delivery address") { fillTangemPayDeliveryAddress() }
            step("Order the plastic card") { clickTangemPayOrderCardButton() }
            step("Assert 'Something went wrong' sheet is displayed") {
                awaitSuccess { onTangemPayOrderFailedSheet { title.assertIsDisplayed() } }
            }
            step("Assert the sheet icon and 'Please try again later.' message are displayed") {
                onTangemPayOrderFailedSheet {
                    icon.assertIsDisplayed()
                    message.assertIsDisplayed()
                }
            }
            step("Assert 'Close' and 'Retry' buttons are displayed") {
                onTangemPayOrderFailedSheet {
                    closeButton.assertIsDisplayed()
                    retryButton.assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("11982")
    @DisplayName("Tangem Pay: a placed plastic card order shows the success screen and starts polling")
    @Test
    fun plasticOrderSuccessStartsPollingTest() {
        setupHooks(
            additionalBeforeSection = {
                setTangemPayPlasticScenarios(customer = noPlasticState, offers = paidDeliveryState)
            }
        ).run {
            step("Open Tangem Pay") { openTangemPay() }
            val ordersBefore = getWireMockRequestCount("POST", placeOrderEndpoint)
            val pollsBefore = getWireMockRequestCount("GET", plasticOrderStatusEndpoint)
            step("Order the plastic card") { orderTangemPayPlasticCard() }
            step("Assert 'Card ordered' screen content") {
                assertTangemPayOrderCardSuccessContent(deliveryEtaMaxDays, email)
            }
            step("Assert 'Show card' button is displayed") {
                onTangemPayOrderCardSuccessScreen { showCardButton.assertIsDisplayed() }
            }
            step("Assert the plastic order request was sent") {
                assertTangemPayRequestSent("POST", placeOrderEndpoint, ordersBefore)
            }
            step("Assert the plastic order status is polled") {
                assertTangemPayRequestSent("GET", plasticOrderStatusEndpoint, pollsBefore)
            }
        }
    }
}