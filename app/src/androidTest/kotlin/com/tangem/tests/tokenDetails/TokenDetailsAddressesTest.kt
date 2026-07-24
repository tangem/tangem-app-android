package com.tangem.tests.tokenDetails

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.assertQrCodeEncodesDisplayedAddress
import com.tangem.scenarios.assertQrCodesMatchForBothAddressTypes
import com.tangem.scenarios.goToQrCodeBottomSheet
import com.tangem.scenarios.openReceiveViaAddFunds
import com.tangem.scenarios.openTokenDetails
import com.tangem.screens.onTokenDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TokenDetailsAddressesTest : BaseTestCase() {

    @AllureId("4947")
    @DisplayName("Token details (address): QR code encodes the displayed address (Bitcoin, 2 address types)")
    @Test
    fun qrCodeEncodesDisplayedAddressBitcoinTest() {
        val tokenName = "Bitcoin"

        setupHooks().run {
            step("Open token details for '$tokenName'") {
                openTokenDetails(tokenName)
            }
            step("Open receive via 'Add funds'") {
                openReceiveViaAddFunds()
            }
            step("Assert QR codes match for both address types") {
                assertQrCodesMatchForBothAddressTypes()
            }
        }
    }

    @AllureId("10218")
    @DisplayName("Token details (address): QR code encodes the displayed address (Cosmos)")
    @Test
    fun qrCodeEncodesDisplayedAddressCosmosTest() {
        val tokenName = "Cosmos"
        val networksProvidersScenario = "networks_providers"
        val appTransfersNetworksState = "AppTransfersNetworks"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(networksProvidersScenario, appTransfersNetworksState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(networksProvidersScenario)
            },
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$appTransfersNetworksState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = appTransfersNetworksState)
            }

            step("Open token details for '$tokenName'") {
                openTokenDetails(tokenName)
            }
            step("Open receive via 'Add funds'") {
                openReceiveViaAddFunds()
            }
            step("Go to QR code bottom sheet") {
                goToQrCodeBottomSheet()
            }
            step("Assert QR code encodes the displayed address") {
                assertQrCodeEncodesDisplayedAddress()
            }
        }
    }

    @AllureId("10219")
    @DisplayName("Token details (address): QR code encodes the displayed address (Kaspa)")
    @Test
    fun qrCodeEncodesDisplayedAddressKaspaTest() {
        val tokenName = "Kaspa"
        val networksProvidersScenario = "networks_providers"
        val appTransfersNetworksState = "AppTransfersNetworks"
        val quotesKaspaState = "Kaspa"
        val kaspaUtxoScenario = "kaspa_utxo"
        val kaspaUtxoState = "more_than_84_android"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(networksProvidersScenario, appTransfersNetworksState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(networksProvidersScenario)
                resetWireMockScenarioState(kaspaUtxoScenario)
            },
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$quotesKaspaState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = quotesKaspaState)
            }
            step("Set WireMock scenario: '$kaspaUtxoScenario' to state: '$kaspaUtxoState'") {
                setWireMockScenarioState(scenarioName = kaspaUtxoScenario, state = kaspaUtxoState)
            }

            step("Open token details for '$tokenName'") {
                openTokenDetails(tokenName)
            }
            step("Open receive via 'Add funds'") {
                openReceiveViaAddFunds()
            }
            step("Go to QR code bottom sheet") {
                goToQrCodeBottomSheet()
            }
            step("Assert QR code encodes the displayed address") {
                assertQrCodeEncodesDisplayedAddress()
            }
        }
    }

    @AllureId("10215")
    @DisplayName("Token details (address): QR code encodes the displayed address (Litecoin, 2 address types)")
    @Test
    fun qrCodeEncodesDisplayedAddressLitecoinTest() {
        val tokenName = "Litecoin"
        val userTokensState = "Litecoin"

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(USER_TOKENS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }

            step("Open token details for '$tokenName'") {
                openTokenDetails(tokenName)
            }
            step("Click on 'Receive' button") {
                onTokenDetailsScreen { receiveButton.clickWithAssertion() }
            }
            step("Assert QR codes match for both address types") {
                assertQrCodesMatchForBothAddressTypes()
            }
        }
    }

    @AllureId("10220")
    @DisplayName("Token details (address): QR code encodes the displayed address (XDC Network, 2 address types)")
    @Test
    fun qrCodeEncodesDisplayedAddressXdcTest() {
        val tokenName = "XDC Network"
        val userTokensState = "XDC"

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(USER_TOKENS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }

            step("Open token details for '$tokenName'") {
                openTokenDetails(tokenName)
            }
            step("Click on 'Receive' button") {
                onTokenDetailsScreen { receiveButton.clickWithAssertion() }
            }
            step("Assert QR codes match for both address types") {
                assertQrCodesMatchForBothAddressTypes()
            }
        }
    }

    @AllureId("10216")
    @DisplayName("Token details (address): QR code encodes the displayed address (Hedera)")
    @Test
    fun qrCodeEncodesDisplayedAddressHederaTest() {
        val tokenName = "Hedera"
        val networksProvidersScenario = "networks_providers"
        val appTransfersNetworksState = "AppTransfersNetworks"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(networksProvidersScenario, appTransfersNetworksState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
                resetWireMockScenarioState(networksProvidersScenario)
            },
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$tokenName'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = tokenName)
            }
            step("Set WireMock scenario: '$QUOTES_API_SCENARIO' to state: '$appTransfersNetworksState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = appTransfersNetworksState)
            }

            step("Open token details for '$tokenName'") {
                openTokenDetails(tokenName)
            }
            step("Open receive via 'Add funds'") {
                openReceiveViaAddFunds()
            }
            step("Go to QR code bottom sheet") {
                goToQrCodeBottomSheet()
            }
            step("Assert QR code encodes the displayed address") {
                assertQrCodeEncodesDisplayedAddress()
            }
        }
    }

    @AllureId("10217")
    @DisplayName("Token details (address): QR code encodes the displayed address (Ethereum)")
    @Test
    fun qrCodeEncodesDisplayedAddressEthereumTest() {
        val tokenName = "Ethereum"

        setupHooks().run {
            step("Open token details for '$tokenName'") {
                openTokenDetails(tokenName)
            }
            step("Open receive via 'Add funds'") {
                openReceiveViaAddFunds()
            }
            step("Go to QR code bottom sheet") {
                goToQrCodeBottomSheet()
            }
            step("Assert QR code encodes the displayed address") {
                assertQrCodeEncodesDisplayedAddress()
            }
        }
    }

    @AllureId("10214")
    @DisplayName("Token details (address): QR code encodes the displayed address (Decimal Smart Chain, 2 address types)")
    @Test
    fun qrCodeEncodesDisplayedAddressDecimalTest() {
        val tokenName = "Decimal Smart Chain"
        val userTokensState = "Decimal"

        setupHooks(
            additionalAfterSection = { resetWireMockScenarioState(USER_TOKENS_API_SCENARIO) },
        ).run {
            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = userTokensState)
            }

            step("Open token details for '$tokenName'") {
                openTokenDetails(tokenName)
            }
            step("Click on 'Receive' action") {
                onTokenDetailsScreen { receiveButton.clickWithAssertion() }
            }
            step("Assert QR codes match for both address types") {
                assertQrCodesMatchForBothAddressTypes()
            }
        }
    }
}