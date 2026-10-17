package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openBuyScreenFromTokenDetails
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class BuyTokenTest : BaseTestCase() {

    @AllureId("3478")
    @DisplayName("Onramp: error in providers loading")
    @Test
    fun errorInProvidersLoadingTest() {
        val scenarioName = "payment_methods"
        val tokenTitle = "Bitcoin"

        setupHooks().run {
            step("Setup WireMock scenario '$scenarioName' for 'Error' state") {
                setWireMockScenarioState(scenarioName, "Error")
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onChooseTokenBottomSheet {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Buy' in 'Get token' bottom sheet") {
                onGetTokenBottomSheet { buyButton.clickWithAssertion() }
            }
            step("Assert error notification title is displayed") {
                onBuyTokenDetailsScreen { errorNotificationTitle.assertIsDisplayed() }
            }
            step("Assert error notification text is displayed") {
                onBuyTokenDetailsScreen { errorNotificationText.assertIsDisplayed() }
            }
            step("Assert 'Refresh' button is displayed and clickable") {
                onBuyTokenDetailsScreen { refreshButton.clickWithAssertion() }
            }
        }
    }

    @AllureId("2565")
    @DisplayName("Onramp: validate currency selector")
    @Test
    fun validateCurrencySelectorTest() {
        setupHooks().run {
            val tokenTitle = "Polygon"
            val popularFiatsTitle = "Popular Fiats"
            val otherCurrenciesTitle = "Other currencies"
            val australianDollar = "AUD"
            val fiatAmount = "1"
            val tokenAmount = "POL 488.24938338"

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onChooseTokenBottomSheet {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Buy' in 'Get token' bottom sheet") {
                onGetTokenBottomSheet { buyButton.clickWithAssertion() }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Write fiat amount = '$fiatAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
            }
            step("Assert offer token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { offerTokenAmount.assertTextContains(tokenAmount) }
            }
            step("Fiat currency icon is displayed") {
                onBuyTokenDetailsScreen { fiatCurrencyIcon.assertIsDisplayed() }
            }
            step("Click on 'Expand fiat list' button") {
                onBuyTokenDetailsScreen { expandFiatListButton.clickWithAssertion() }
            }
            step("Assert '$popularFiatsTitle' is displayed") {
                onBuyTokenFiatListBottomSheet {
                    fiatListItemWithTitle(popularFiatsTitle).assertIsDisplayed()
                }
            }
            step("Assert '$otherCurrenciesTitle' is displayed") {
                onBuyTokenFiatListBottomSheet {
                    fiatListItemWithTitle(otherCurrenciesTitle).assertIsDisplayed()
                }
            }
            step("Click on fiat with title: '$australianDollar'") {
                onBuyTokenFiatListBottomSheet {
                    fiatListItemWithTitle(australianDollar).performClick()
                }
            }
            step("Assert new fiat currency: '$australianDollar' is displayed") {
                onBuyTokenDetailsScreen {
                    fiatAmountTextField.assertTextContains(australianDollar + fiatAmount)
                }
            }
            step("Assert offer token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { offerTokenAmount.assertTextContains(tokenAmount) }
            }
        }
    }

    @AllureId("11991")
    @DisplayName("Buy. Currency. Currencies list was not received")
    @Test
    fun currenciesListNotReceivedTest() {
        val scenarioName = "currencies_api"
        val scenarioState = "Error"
        val currenciesMethod = "GET"
        val currenciesPath = "/v1/currencies"
        val tokenTitle = "Polygon"

        setupHooks().run {
            step("Set WireMock scenario '$scenarioName' to '$scenarioState'") {
                setWireMockScenarioState(scenarioName, scenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onChooseTokenBottomSheet {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Buy' in 'Get token' bottom sheet") {
                onGetTokenBottomSheet { buyButton.clickWithAssertion() }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            val requestsBeforeOpen = getWireMockRequestCount(currenciesMethod, currenciesPath)
            step("Click on 'Expand fiat list' button") {
                onBuyTokenDetailsScreen { expandFiatListButton.clickWithAssertion() }
            }
            step("Assert error message is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onBuyTokenFiatListBottomSheet { errorTitle.assertIsDisplayed() } }
            }
            step("Assert 'Try again' button is displayed") {
                onBuyTokenFiatListBottomSheet { tryAgainButton.assertIsDisplayed() }
            }
            step("Assert currencies were requested on opening the list") {
                flakySafely {
                    assertTrue(getWireMockRequestCount(currenciesMethod, currenciesPath) > requestsBeforeOpen)
                }
            }
            val requestsAfterOpen = getWireMockRequestCount(currenciesMethod, currenciesPath)
            step("Click on 'Try again' button") {
                onBuyTokenFiatListBottomSheet { tryAgainButton.clickWithAssertion() }
            }
            step("Assert currencies were requested again") {
                flakySafely {
                    assertTrue(getWireMockRequestCount(currenciesMethod, currenciesPath) > requestsAfterOpen)
                }
            }
            step("Assert error message is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onBuyTokenFiatListBottomSheet { errorTitle.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("3719")
    @DisplayName("Buy. All offers. Offers sorting")
    @Test
    fun allOffersSortedByReceiveAmountTest() {
        val quoteScenarioName = "onramp_quote"
        val quoteScenarioState = "ProvidersSorting"
        val quotesPath = "/v1/onramp-quote"
        val tokenTitle = "Polygon"
        val fiatAmount = "1"
        val paymentMethod = "Card"
        val paymentMethodId = "card"
        val lowerAmountProviderId = "mercuryo"
        val higherAmountProviderId = "simplex"
        val higherAmountProviderName = "Simplex"
        val lowerAmountProviderName = "Mercuryo"
        val higherAmount = "520.00"
        val lowerAmount = "488.24938338"

        setupHooks().run {
            step("Set WireMock scenario '$quoteScenarioName' to '$quoteScenarioState'") {
                setWireMockScenarioState(quoteScenarioName, quoteScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            val lowerProviderQuotesBefore = getWireMockRequestCountByQueryParam(
                method = "GET",
                urlPath = quotesPath,
                queryParam = "providerId",
                queryValue = lowerAmountProviderId,
            )
            val higherProviderQuotesBefore = getWireMockRequestCountByQueryParam(
                method = "GET",
                urlPath = quotesPath,
                queryParam = "providerId",
                queryValue = higherAmountProviderId,
            )
            val cardQuotesBefore = getWireMockRequestCountByQueryParam(
                method = "GET",
                urlPath = quotesPath,
                queryParam = "paymentMethod",
                queryValue = paymentMethodId,
            )
            step("Open 'Buy' screen from token details and write amount") {
                openBuyScreenFromTokenDetails(tokenTitle = tokenTitle, fiatAmount = fiatAmount)
            }
            step("Assert offer token amount is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onBuyTokenDetailsScreen { offerTokenAmount.assertIsDisplayed() } }
            }
            step("Assert quote of '$lowerAmountProviderId' was requested") {
                flakySafely {
                    assertTrue(
                        getWireMockRequestCountByQueryParam("GET", quotesPath, "providerId", lowerAmountProviderId) >
                            lowerProviderQuotesBefore,
                    )
                }
            }
            step("Assert quote of '$higherAmountProviderId' was requested") {
                flakySafely {
                    assertTrue(
                        getWireMockRequestCountByQueryParam("GET", quotesPath, "providerId", higherAmountProviderId) >
                            higherProviderQuotesBefore,
                    )
                }
            }
            step("Assert both providers were requested with payment method '$paymentMethodId'") {
                flakySafely {
                    assertTrue(
                        getWireMockRequestCountByQueryParam("GET", quotesPath, "paymentMethod", paymentMethodId) >=
                            cardQuotesBefore + 2,
                    )
                }
            }
            step("Click on 'All offers' button") {
                onBuyTokenDetailsScreen { allOffersButton.clickWithAssertion() }
            }
            step("Click on '$paymentMethod' payment method") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithName(paymentMethod).clickWithAssertion() }
            }
            step("Assert provider '$higherAmountProviderName' is listed") {
                onSelectProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        providerNameContaining(higherAmountProviderName).assertIsDisplayed()
                    }
                }
            }
            step("Assert provider '$lowerAmountProviderName' is listed") {
                onSelectProviderBottomSheet { providerNameContaining(lowerAmountProviderName).assertIsDisplayed() }
            }
            step("Assert offers with '$higherAmount' are above offers with '$lowerAmount'") {
                onSelectProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { assertOfferAmountsOrdered(higherAmount, lowerAmount) }
                }
            }
        }
    }

    @AllureId("11588")
    @DisplayName("Buy. Geo. Restrictions for a country and one provider")
    @Test
    fun oneProviderRestrictedTest() {
        val quoteScenarioName = "onramp_quote"
        val quoteScenarioState = "ProviderRestricted"
        val tokenTitle = "Polygon"
        val fiatAmount = "1"
        val paymentMethod = "Card"

        setupHooks().run {
            step("Set WireMock scenario '$quoteScenarioName' to '$quoteScenarioState'") {
                setWireMockScenarioState(quoteScenarioName, quoteScenarioState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Buy' screen from token details and write amount") {
                openBuyScreenFromTokenDetails(tokenTitle = tokenTitle, fiatAmount = fiatAmount)
            }
            step("Assert offer token amount is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { onBuyTokenDetailsScreen { offerTokenAmount.assertIsDisplayed() } }
            }
            step("Assert region restriction error is not displayed under the amount") {
                onBuyTokenDetailsScreen { regionRestrictionAmountError.assertDoesNotExist() }
            }
            step("Click on 'All offers' button") {
                onBuyTokenDetailsScreen { allOffersButton.clickWithAssertion() }
            }
            step("Click on '$paymentMethod' payment method") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithName(paymentMethod).clickWithAssertion() }
            }
            step("Assert the restricted provider has 'Not available' badge") {
                onSelectProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { restrictedOfferBadge.assertIsDisplayed() }
                }
            }
            step("Assert 'Buy' button of the restricted provider is disabled") {
                onSelectProviderBottomSheet { disabledOfferBuyButton.assertIsDisplayed() }
            }
            step("Assert 'Buy' button of the other provider is enabled") {
                onSelectProviderBottomSheet { enabledOfferBuyButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("2566")
    @DisplayName("Onramp: validate 'Buy token' screen")
    @Test
    fun validateBuyTokenScreenTest() {
        setupHooks().run {
            val tokenTitle = "Polygon"
            val euro = "EUR"
            val fiatAmount = "1"
            val tokenAmount = "POL 488.24938338"

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onChooseTokenBottomSheet {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Buy' in 'Get token' bottom sheet") {
                onGetTokenBottomSheet { buyButton.clickWithAssertion() }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Assert 'Buy Token' title is displayed") {
                onBuyTokenDetailsScreen { topBarTitle.assertTextContains("Buy $tokenTitle") }
            }
            step("Assert 'More button' in top bar is displayed") {
                onBuyTokenDetailsScreen { topBarMoreButton.assertIsDisplayed() }
            }
            step("Write fiat amount = '$fiatAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
            }
            step("Assert fiat amount = '$fiatAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.assertTextContains(euro + fiatAmount) }
            }
            step("Assert offer token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { offerTokenAmount.assertTextContains(tokenAmount) }
            }
            step("Assert 'Recommended' title is displayed") {
                onBuyTokenDetailsScreen { recommendedTitle.clickWithAssertion() }
            }
            step("Assert 'Best rate' title is displayed") {
                onBuyTokenDetailsScreen { bestRateTitle.assertIsDisplayed() }
            }
            step("Assert offer amount is displayed") {
                onBuyTokenDetailsScreen { offerTokenAmount.assertIsDisplayed() }
            }
            step("Assert 'Buy' button is displayed") {
                onBuyTokenDetailsScreen { buyButton.assertIsDisplayed()}
            }
            step("Assert timing icon is displayed") {
                onBuyTokenDetailsScreen { timingIcon.assertIsDisplayed() }
            }
            step("Assert 'Instant' processing time text is displayed") {
                onBuyTokenDetailsScreen { instantProcessingSpeedText.assertIsDisplayed() }
            }
            step("Assert provider name is displayed") {
                onBuyTokenDetailsScreen { providerName.assertIsDisplayed() }
            }
            step("Assert 'Pay with' is displayed") {
                onBuyTokenDetailsScreen { payWith.assertIsDisplayed() }
            }
            step("Assert payment method icon is displayed") {
                onBuyTokenDetailsScreen { paymentMethodIcon.assertIsDisplayed() }
            }
            step("Assert 'All offers' button is displayed") {
                onBuyTokenDetailsScreen { allOffersButton.assertIsDisplayed() }
            }
            step("Assert 'Close' button in top bar is displayed") {
                onBuyTokenDetailsScreen { topBarCloseButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("2563")
    @DisplayName("Onramp: validate 'Residence' settings screen")
    @Test
    fun validateResidenceSettingsScreenTest() {
        setupHooks().run {
            val tokenTitle = "Polygon"
            val country = "Albania"
            val unavailableCountry = "Lebanon"

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onChooseTokenBottomSheet {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Buy' in 'Get token' bottom sheet") {
                onGetTokenBottomSheet { buyButton.clickWithAssertion() }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Assert 'Buy $tokenTitle' title is displayed") {
                onBuyTokenDetailsScreen { topBarTitle.assertTextContains("Buy $tokenTitle") }
            }
            step("Click 'More' button in tab bar") {
                onBuyTokenDetailsScreen { topBarMoreButton.clickWithAssertion() }
            }
            step("Assert 'Residence Settings' screen top bar title is displayed") {
                onResidenceSettingsScreen { topBarTitle.assertIsDisplayed() }
            }
            step("Assert 'Residence Settings' screen top bar 'Close' button is displayed") {
                onResidenceSettingsScreen { topBarCloseButton.assertIsDisplayed() }
            }
            step("Assert 'Residence' button is displayed on 'Residence Settings' screen") {
                onResidenceSettingsScreen { residenceButton.assertIsDisplayed() }
            }
            step("Assert country name is displayed on 'Residence Settings' screen") {
                onResidenceSettingsScreen { countryName.assertIsDisplayed() }
            }
            step("Assert residence settings description is displayed on 'Residence Settings' screen") {
                onResidenceSettingsScreen { residenceSettingsDescription.assertIsDisplayed() }
            }
            step("Click 'Residence button'") {
                onResidenceSettingsScreen { residenceButton.clickWithAssertion() }
            }
            step("Assert 'Search bar' is displayed") {
                onSelectCountryBottomSheet { searchBar.assertIsDisplayed() }
            }
            step("Type unavailable country name: '$unavailableCountry' in 'Search bar'") {
                onSelectCountryBottomSheet { searchBar.performTextReplacement(unavailableCountry) }
            }
            step("Unavailable country: '$unavailableCountry' is displayed") {
                onSelectCountryBottomSheet { unavailableCountryWithNameAndIcon(unavailableCountry).assertIsDisplayed() }
            }
            step("Type country name: '$country' in 'Search bar'") {
                onSelectCountryBottomSheet { searchBar.performTextReplacement(country) }
            }
            step("Available country: '$country' is displayed") {
                onSelectCountryBottomSheet { countryWithNameAndIcon(country).assertIsDisplayed() }
            }
            step("Click on country: '$country'") {
                onSelectCountryBottomSheet { countryWithNameAndIcon(country).clickWithAssertion() }
            }
            step("Assert country: '$country' is displayed on 'Residence Settings' screen") {
                onResidenceSettingsScreen { countryName.assertTextContains(country) }
            }
        }
    }

    @AllureId("2570")
    @DisplayName("Onramp: validate 'Select provider' bottom sheet")
    @Test
    fun validateProvidersScreenTest() {
        setupHooks().run {
            val tokenTitle = "Polygon"
            val paymentMethod = "Invoice Revolut Pay"
            val fiatAmount = "1"
            val providerNameMercuryo = "Mercuryo"
            val tokenAmount = "POL 488.24938338"

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onChooseTokenBottomSheet {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Buy' in 'Get token' bottom sheet") {
                onGetTokenBottomSheet { buyButton.clickWithAssertion() }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Write fiat amount = '$fiatAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
            }
            step("Assert offer token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { offerTokenAmount.assertTextContains(tokenAmount) }
            }
            step("Assert 'Provider block' is displayed") {
                onBuyTokenDetailsScreen { providerName.assertIsDisplayed() }
            }
            step("Click on 'All offers' button") {
                onBuyTokenDetailsScreen { allOffersButton.clickWithAssertion() }
            }
            step("Click on '$paymentMethod' payment method") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithName(paymentMethod).clickWithAssertion() }
            }
            step("Assert 'Provider' bottom sheet title is displayed") {
                onSelectProviderBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert 'Provider' bottom sheet subtitle is displayed") {
                onSelectProviderBottomSheet { subtitle.assertIsDisplayed() }
            }
            step("Assert 'Best rate' title is displayed") {
                onSelectProviderBottomSheet { bestRateTitle.assertIsDisplayed() }
            }
            step("Assert offer token amount is displayed") {
                onSelectProviderBottomSheet { offerTokenAmount.assertIsDisplayed() }
            }
            step("Assert 'Buy' button is displayed") {
                onSelectProviderBottomSheet { buyButton.assertIsDisplayed() }
            }
            step("Assert timing icon is displayed") {
                onSelectProviderBottomSheet { timingIcon.assertIsDisplayed() }
            }
            step("Assert 'Instant' processing time text is displayed") {
                onSelectProviderBottomSheet { instantProcessingSpeedText.assertIsDisplayed() }
            }
            step("Assert 'Pay with' text is displayed") {
                onSelectProviderBottomSheet { payWith.assertIsDisplayed() }
            }
            step("Assert payment method icon is displayed") {
                onSelectProviderBottomSheet { paymentMethodIcon.assertIsDisplayed() }
            }
            step("Assert provider name: '$providerNameMercuryo'") {
                onSelectProviderBottomSheet {
                    providerName.assertTextContainsSafe(text = providerNameMercuryo, substring = true)
                }
            }
        }
    }

    @AllureId("3479")
    @DisplayName("Onramp: validate 'Select payment method' bottom sheet")
    @Test
    fun validatePaymentMethodScreenTest() {
        setupHooks().run {
            val tokenTitle = "Polygon"
            val card = "Card"
            val googlePay = "Google Pay"
            val invoiceRevolutPay = "Invoice Revolut Pay"
            val sepa = "Sepa"
            val fiatAmount = "1"
            val tokenAmount = "POL 488.24938338"

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Add funds' button") {
                onMainScreen { addFundsButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onChooseTokenBottomSheet {
                    title.assertIsDisplayed()
                    tokenWithTitle(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Buy' in 'Get token' bottom sheet") {
                onGetTokenBottomSheet { buyButton.clickWithAssertion() }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Write fiat amount = '$fiatAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
            }
            step("Assert offer token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { offerTokenAmount.assertTextContains(tokenAmount) }
            }
            step("Assert 'Provider block' is displayed") {
                onBuyTokenDetailsScreen { providerName.assertIsDisplayed() }
            }
            step("Click on 'All offers' button") {
                onBuyTokenDetailsScreen { allOffersButton.clickWithAssertion() }
            }
            step("Assert 'Select Payment Method' bottom sheet title is displayed") {
                onSelectPaymentMethodBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert 'Select Payment Method' bottom sheet subtitle is displayed") {
                onSelectPaymentMethodBottomSheet { subtitle.assertIsDisplayed() }
            }
            step("Assert 'Select Payment Method' bottom sheet close button is displayed") {
                onSelectPaymentMethodBottomSheet { closeButton.assertIsDisplayed() }
            }
            step("Assert payment method: '$card' is displayed") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithName(card).assertIsDisplayed() }
            }
            step("Assert payment method: '$googlePay' is displayed") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithName(googlePay).assertIsDisplayed() }
            }
            step("Assert payment method: '$invoiceRevolutPay' is displayed") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithName(invoiceRevolutPay).assertIsDisplayed() }
            }
            step("Assert payment method: '$sepa' is displayed") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithName(sepa).assertIsDisplayed() }
            }
        }
    }
}