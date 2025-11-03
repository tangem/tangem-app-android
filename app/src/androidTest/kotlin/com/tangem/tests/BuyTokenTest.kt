package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class BuyTokenTest : BaseTestCase() {

    @AllureId("3478")
    @DisplayName("Onramp: error in providers loading")
    @Test
    fun errorInProvidersLoadingTest() {
        val scenarioName = "payment_methods"
        val tokenTitle = "Bitcoin"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(scenarioName)
            }
        ).run {
            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }
            step("Setup WireMock scenario '$scenarioName' for 'Error' state") {
                setWireMockScenarioState(scenarioName, "Error")
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onBuyTokenScreen {
                    topAppBarTitle.assertIsDisplayed()
                    tokenWithTitleAndFiatAmount(tokenTitle).clickWithAssertion()
                }
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
            val tokenAmount = "~488.24938338 POL"
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onBuyTokenScreen {
                    topAppBarTitle.assertIsDisplayed()
                    tokenWithTitleAndFiatAmount(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Write fiat amount = '$fiatAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
            }
            step("Assert token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen {
                    tokenAmountField.assertTextContains(tokenAmount)
                }
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
            step("Assert token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen {
                    tokenAmountField.assertTextContains(tokenAmount)
                }
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
            val tokenAmount = "~488.24938338 POL"
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onBuyTokenScreen {
                    topAppBarTitle.assertIsDisplayed()
                    tokenWithTitleAndFiatAmount(tokenTitle).clickWithAssertion()
                }
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
            step("Assert token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { tokenAmountField.assertTextContains(tokenAmount) }
            }
            step("Click on 'Continue' button") {
                onBuyTokenDetailsScreen { continueButton.clickWithAssertion() }
            }
            step("Assert 'Recommended' title is displayed") {
                onBuyTokenDetailsScreen { recommendedTitle.clickWithAssertion() }
            }
            step("Assert 'Best rate' icon is displayed") {
                onBuyTokenDetailsScreen { bestRateIcon.assertIsDisplayed() }
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
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onBuyTokenScreen {
                    topAppBarTitle.assertIsDisplayed()
                    tokenWithTitleAndFiatAmount(tokenTitle).clickWithAssertion()
                }
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
            val tokenAmount = "~488.24938338 POL"
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onBuyTokenScreen {
                    topAppBarTitle.assertIsDisplayed()
                    tokenWithTitleAndFiatAmount(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Write fiat amount = '$fiatAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
            }
            step("Assert token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { tokenAmountField.assertTextContains(tokenAmount) }
            }
            step("Click on 'Continue' button") {
                onBuyTokenDetailsScreen { continueButton.clickWithAssertion() }
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
            step("Assert 'Best rate' icon is displayed") {
                onSelectProviderBottomSheet { bestRateIcon.assertIsDisplayed() }
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
                onSelectProviderBottomSheet { providerName.assertTextContains(providerNameMercuryo) }
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
            val tokenAmount = "~488.24938338 POL"
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Click on 'Buy' button") {
                onMainScreen { buyButton.clickWithAssertion() }
            }
            step("Click on token with name: '$tokenTitle'") {
                onBuyTokenScreen {
                    topAppBarTitle.assertIsDisplayed()
                    tokenWithTitleAndFiatAmount(tokenTitle).clickWithAssertion()
                }
            }
            step("Click on 'Confirm' button in 'Dialog'") {
                onDialog { confirmButton.clickWithAssertion() }
            }
            step("Write fiat amount = '$fiatAmount'") {
                onBuyTokenDetailsScreen { fiatAmountTextField.performTextInput(fiatAmount) }
            }
            step("Assert token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { tokenAmountField.assertTextContains(tokenAmount) }
            }
            step("Click on 'Continue' button") {
                onBuyTokenDetailsScreen { continueButton.clickWithAssertion() }
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