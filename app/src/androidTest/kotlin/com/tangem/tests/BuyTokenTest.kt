package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.screens.*
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
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
        val balance = TOTAL_BALANCE

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
                synchronizeAddresses(balance)
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
            val balance = TOTAL_BALANCE
            val popularFiatsTitle = "Popular Fiats"
            val otherCurrenciesTitle = "Other currencies"
            val australianDollar = "AUD"
            val fiatAmount = "1"
            val tokenAmount = "POL 488.24938338"
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
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
            step("Assert 'Provider loading block' is displayed") {
                onBuyTokenDetailsScreen {
                    providerLoadingTitle.assertIsDisplayed()
                    providerLoadingText.assertIsDisplayed()
                }
            }
            step("Assert 'Provider block' is displayed") {
                onBuyTokenDetailsScreen {
                    providerTitle.assertIsDisplayed()
                    providerText.assertIsDisplayed()
                }
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
            val balance = TOTAL_BALANCE
            val euro = "EUR"
            val fiatAmount = "1"
            val tokenAmount = "POL 488.24938338"
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
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
            step("Assert 'Provider loading block' is displayed") {
                onBuyTokenDetailsScreen {
                    providerLoadingTitle.assertIsDisplayed()
                    providerLoadingText.assertIsDisplayed()
                }
            }
            step("Assert 'Provider block' is displayed") {
                onBuyTokenDetailsScreen {
                    providerTitle.assertIsDisplayed()
                    providerText.assertIsDisplayed()
                }
            }
            step("Assert token amount = '$tokenAmount'") {
                onBuyTokenDetailsScreen { tokenAmountField.assertTextContains(tokenAmount) }
            }
            step("Assert 'ToS' block is displayed") {
                onBuyTokenDetailsScreen { toSBlock.assertIsDisplayed()}
            }
            step("Assert 'Buy' button is displayed") {
                onBuyTokenDetailsScreen { buyButton.assertIsDisplayed()}
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
            val balance = TOTAL_BALANCE
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
                synchronizeAddresses(balance)
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
            val balance = TOTAL_BALANCE
            val paymentMethod = "Card"
            val fiatAmount = "1"
            val providerNameMercuryo = "Mercuryo"
            val providerNameSimplex = "Simplex"
            val tokenAmount = "POL 488.24938338"
            val bestRate = "Best rate"
            val rate = "-0.00%"
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
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
            step("Assert 'Provider block' is displayed") {
                onBuyTokenDetailsScreen {
                    providerTitle.assertIsDisplayed()
                    providerText.assertIsDisplayed()
                }
            }
            step("Open 'Select Provider' bottom sheet") {
                onBuyTokenDetailsScreen { providerTitle.performClick() }
            }
            step("Assert available provider name is displayed") {
                onSelectProviderBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) {
                        availableProviderItem.assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Expand payment methods' button") {
                onSelectProviderBottomSheet { paymentMethodExpandButton.clickWithAssertion() }
            }
            step("Click on payment method: '$paymentMethod'") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithNameAndIcon(paymentMethod).clickWithAssertion() }
            }
            step("Assert 'Select Provider' bottom sheet title is displayed") {
                onSelectProviderBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert payment method icon is displayed") {
                onSelectProviderBottomSheet { paymentMethodIcon.assertIsDisplayed() }
            }
            step("Assert payment method title is displayed") {
                onSelectProviderBottomSheet { paymentMethodTitle.assertIsDisplayed() }
            }
            step("Assert payment method name is displayed") {
                onSelectProviderBottomSheet { paymentMethodName.assertIsDisplayed() }
            }
            step("Assert provider with name: '$providerNameMercuryo' and rate: '$bestRate' is displayed") {
                onSelectProviderBottomSheet {
                    availableProviderWithName(providerNameMercuryo, tokenAmount, bestRate).assertIsDisplayed()
                }
            }
            step("Assert provider with name: '$providerNameSimplex' and rate: '$rate' is displayed") {
                onSelectProviderBottomSheet {
                    availableProviderWithName(providerNameSimplex, tokenAmount, rate).assertIsDisplayed()
                }
            }
            step("Assert 'More providers' icon is displayed") {
                onSelectProviderBottomSheet { moreProvidersIcon.assertIsDisplayed() }
            }
            step("Assert 'More providers' text is displayed") {
                onSelectProviderBottomSheet { moreProvidersText.assertIsDisplayed() }
            }
            step("Assert 'Best rate' label is displayed") {
                onSelectProviderBottomSheet { bestRateLabel.assertIsDisplayed() }
            }
        }
    }

    @AllureId("3479")
    @DisplayName("Onramp: validate 'Select payment method' bottom sheet")
    @Test
    fun validatePaymentMethodScreenTest() {
        setupHooks().run {
            val tokenTitle = "Polygon"
            val balance = TOTAL_BALANCE
            val card = "Card"
            val googlePay = "Google Pay"
            val invoiceRevolutPay = "Invoice Revolut Pay"
            val sepa = "Sepa"
            val fiatAmount = "1"
            val scenarioName = "payment_methods"

            step("Reset WireMock scenario '$scenarioName'") {
                resetWireMockScenarioState(scenarioName)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
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
            step("Assert 'Provider block' is displayed") {
                onBuyTokenDetailsScreen {
                    providerTitle.assertIsDisplayed()
                    providerText.assertIsDisplayed()
                }
            }
            step("Open 'Select Provider' bottom sheet") {
                onBuyTokenDetailsScreen { providerTitle.performClick() }
            }
            step("Click on 'Expand payment methods' button") {
                onSelectProviderBottomSheet { paymentMethodExpandButton.clickWithAssertion() }
            }
            step("Assert 'Select Payment Method' bottom sheet title is displayed") {
                onSelectPaymentMethodBottomSheet { title.assertIsDisplayed() }
            }
            step("Assert payment method: '$card' is displayed") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithNameAndIcon(card).assertIsDisplayed() }
            }
            step("Assert payment method: '$googlePay' is displayed") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithNameAndIcon(googlePay).assertIsDisplayed() }
            }
            step("Assert payment method: '$invoiceRevolutPay' is displayed") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithNameAndIcon(invoiceRevolutPay).assertIsDisplayed() }
            }
            step("Assert payment method: '$sepa' is displayed") {
                onSelectPaymentMethodBottomSheet { paymentMethodWithNameAndIcon(sepa).assertIsDisplayed() }
            }
            step("Press 'Back' button") {
                onSelectPaymentMethodBottomSheet { device.uiDevice.pressBack() }
            }
            step("Assert 'Select Provider' bottom sheet title is displayed") {
                onSelectProviderBottomSheet { title.assertIsDisplayed() }
            }
        }
    }

}