package com.tangem.tests.send.feeScreen

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.ETHEREUM_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.QUOTES_API_SCENARIO
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TERRA_RECIPIENT_ADDRESS
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

/**
 * Completing a send paid with a fee in the token itself (no native fee coin), on a hot wallet:
 * VeChain's VeThor and Terra Classic's TerraClassicUSD.
 */
@HiltAndroidTest
class SendTokenFeeTest : BaseTestCase() {

    private val tokenAmount = "1"

    @AllureId("4907")
    @DisplayName("Send (Fee in token): send VeThor and complete the transaction")
    @Test
    fun sendVeThorWithFeeInTokenTest() {
        val tokenName = "VeThor"
        val scenarioState = "Vechain"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$scenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = scenarioState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$scenarioState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = scenarioState)
            }
            step("Open the send flow for '$tokenName' on an existing hot wallet") {
                openSendScreenWithHotWallet(seedPhrase = SVS_SEED_PHRASE_12, tokenName = tokenName)
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = ETHEREUM_RECIPIENT_ADDRESS)
            }
            assertNetworkFeeContains("$")
            waitUntilNetworkFeeIsStable { readNetworkFeeAmount() }
            step("Sign, send and open the 'Transaction sent' screen") {
                openSendSuccessScreenViaLongClickOnSendButton()
            }
        }
    }

    @AllureId("4908")
    @DisplayName("Send (Fee in token): send TerraClassicUSD and complete the transaction")
    @Test
    fun sendTerraClassicUsdWithFeeInTokenTest() {
        val tokenName = "TerraClassicUSD"
        val scenarioState = "Terra"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(QUOTES_API_SCENARIO)
            }
        ).run {
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$scenarioState'") {
                setWireMockScenarioState(scenarioName = USER_TOKENS_API_SCENARIO, state = scenarioState)
            }
            step("Set WireMock scenario '$QUOTES_API_SCENARIO' to '$scenarioState'") {
                setWireMockScenarioState(scenarioName = QUOTES_API_SCENARIO, state = scenarioState)
            }
            step("Open the send flow for '$tokenName' on an existing hot wallet") {
                openSendScreenWithHotWallet(seedPhrase = SVS_SEED_PHRASE_12, tokenName = tokenName)
            }
            step("Enter amount '$tokenAmount' and open the 'Send confirm' screen") {
                enterAmountAndOpenSendConfirm(amount = tokenAmount, recipientAddress = TERRA_RECIPIENT_ADDRESS)
            }
            assertNetworkFeeContains("$")
            waitUntilNetworkFeeIsStable { readNetworkFeeAmount() }
            step("Sign, send and open the 'Transaction sent' screen") {
                openSendSuccessScreenViaLongClickOnSendButton()
            }
        }
    }
}