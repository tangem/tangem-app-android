package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.onSwapSuccessScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Test

@HiltAndroidTest
class MoneyAccountWithdrawSwapTamperingTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val userTokensUsdcState = "TangemPayTransferUsdc"
    private val storiesScenario = "stories_first_time_swap_v2"
    private val storiesErrorState = "Error"
    private val exchangeDataScenario = "money_account_withdraw_exchange_data"
    private val mainAccountName = getResourceString(CoreResR.string.account_main_account_title)
    private val receiveTokenSymbol = "POL"
    private val polygon = "Polygon"
    private val accountBalance = "25"
    private val amount = "1"
    private val exchangeTxTo = "0x0000000000000000000000000000000000000005"

    @AllureId("13445")
    @DisplayName("Money account: the swap from the account signs the transfer to txTo from exchange-data")
    @Test
    fun swapSignsTransferToExchangeTxToTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            val routesBefore = countMoneyAccountBankWithdrawRoutes()
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open the swap of '$amount' USDC to '$receiveTokenSymbol' of '$mainAccountName'") {
                openMoneyAccountWithdrawSwapWithAmount(accountBalance, mainAccountName, receiveTokenSymbol, polygon, amount)
            }
            step("Confirm swap by holding the button") { confirmTangemPayWithdrawByHolding() }
            step("Assert 'Swap in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Assert $amount USDC is signed to txTo '$exchangeTxTo'") {
                assertEquals(
                    listOf(polygonUsdcTransfer(exchangeTxTo, amount)),
                    findMoneyAccountTransfersSignedSince(signsBefore),
                )
            }
            step("Assert no bank-offramp route is created") {
                assertEquals(routesBefore, countMoneyAccountBankWithdrawRoutes())
            }
        }
    }

    @AllureId("13446")
    @DisplayName("Money account: a substituted txTo in exchange-data is what the swap from the account signs")
    @Test
    fun substitutedTxToIsSignedTest() {
        val tamperedTxToState = "TamperedTxTo"
        val tamperedTxTo = "0x00000000000000000000000000000000deadbeef"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$exchangeDataScenario' to '$tamperedTxToState'") {
                setWireMockScenarioState(exchangeDataScenario, tamperedTxToState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open the swap of '$amount' USDC to '$receiveTokenSymbol' of '$mainAccountName'") {
                openMoneyAccountWithdrawSwapWithAmount(accountBalance, mainAccountName, receiveTokenSymbol, polygon, amount)
            }
            step("Confirm swap by holding the button") { confirmTangemPayWithdrawByHolding() }
            step("Assert 'Swap in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Assert $amount USDC is signed to the substituted txTo '$tamperedTxTo'") {
                assertEquals(
                    listOf(polygonUsdcTransfer(tamperedTxTo, amount)),
                    findMoneyAccountTransfersSignedSince(signsBefore),
                )
            }
        }
    }

    @AllureId("13447")
    @DisplayName("Money account: a substituted txExtraId in exchange-data does not get into the signed transfer")
    @Test
    fun substitutedTxExtraIdIsNotSignedTest() {
        val tamperedTxExtraIdState = "TamperedTxExtraId"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$exchangeDataScenario' to '$tamperedTxExtraIdState'") {
                setWireMockScenarioState(exchangeDataScenario, tamperedTxExtraIdState)
            }
            val signsBefore = countMoneyAccountTransferSignRequests()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open the swap of '$amount' USDC to '$receiveTokenSymbol' of '$mainAccountName'") {
                openMoneyAccountWithdrawSwapWithAmount(accountBalance, mainAccountName, receiveTokenSymbol, polygon, amount)
            }
            step("Confirm swap by holding the button") { confirmTangemPayWithdrawByHolding() }
            step("Assert 'Swap in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Assert exactly a $amount USDC transfer to txTo '$exchangeTxTo' is signed, without a memo") {
                assertEquals(
                    listOf(polygonUsdcTransfer(exchangeTxTo, amount)),
                    findMoneyAccountTransfersSignedSince(signsBefore),
                )
            }
        }
    }
}