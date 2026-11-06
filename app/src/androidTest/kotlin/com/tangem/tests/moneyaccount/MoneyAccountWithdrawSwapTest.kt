package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MoneyAccountWithdrawSwapTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val userTokensUsdcState = "TangemPayTransferUsdc"
    private val storiesScenario = "stories_first_time_swap_v2"
    private val storiesErrorState = "Error"
    private val usdc = "USDC"
    private val polygon = "Polygon"
    private val mainAccountName = getResourceString(CoreResR.string.account_main_account_title)

    @AllureId("13361")
    @DisplayName("Money account: 'Within your portfolio' swaps the account USDC and adds the token to recents")
    @Test
    fun withinPortfolioSwapsAccountUsdcTest() {
        val swapSectionSubtitle = getResourceString(CoreResR.string.tangempay_withdraw_hub_swap_section_subtitle)
        val balanceScenario = "money_account_usdc_balance"
        val balanceAfterWithdrawState = "AfterWithdraw"
        val accountBalance = "25"
        val balanceAfterWithdraw = "$22.49"
        val receiveTokenSymbol = "POL"
        val receiveTokenName = "Polygon"
        val amount = "1"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Assert '$swapSectionSubtitle' subtitle is not displayed") {
                onTangemPayWithdrawHubScreen { hubText(swapSectionSubtitle).assertDoesNotExist() }
            }
            step("Click on 'Within your portfolio' card") {
                onTangemPayWithdrawHubScreen { withinPortfolioCard.performClick() }
            }
            step("Assert 'Withdraw' Swap screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }
            }
            step("Assert source token is '$usdc'") {
                onSwapTokenScreen { swapTokenSymbol(usdc).assertIsDisplayed() }
            }
            step("Assert source balance contains '$accountBalance'") {
                flakySafely { onSwapTokenScreen { swapCardBalance.assertTextContains(accountBalance, substring = true) } }
            }
            step("Assert source token cannot be changed") {
                onSwapTokenScreen { swapSelectTokenIcon.assertDoesNotExist() }
            }
            step("Choose '$receiveTokenName' of '$mainAccountName' as the receive token") {
                chooseMoneyAccountWithdrawReceiveToken(mainAccountName, receiveTokenSymbol, polygon)
            }
            step("Enter withdraw amount '$amount'") { enterWithdrawAmount(amount) }
            step("Assert receive amount is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { receiveAmount.assertIsDisplayed() } }
            }
            step("Confirm swap by holding the button") { confirmTangemPayWithdrawByHolding() }
            step("Assert 'Swap in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Set WireMock scenario '$balanceScenario' to '$balanceAfterWithdrawState'") {
                setWireMockScenarioState(balanceScenario, balanceAfterWithdrawState)
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Assert 'Send' hub screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
            }
            step("Return to 'Money account' screen") { returnToMoneyAccountFromSendHub() }
            step("Refresh 'Money account' screen until the balance is '$balanceAfterWithdraw'") {
                refreshMoneyAccountUntilBalance(balanceAfterWithdraw)
            }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Assert recent token '$receiveTokenName' is displayed in 'Transfer or swap'") {
                flakySafely { onTangemPayWithdrawHubScreen { recentToken(receiveTokenName).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("13372")
    @DisplayName("Money account: Swap from the account blocks an amount above the balance and Max keeps the USDC fee")
    @Test
    fun swapAmountAboveBalanceAndMaxTest() {
        val balanceScenario = "money_account_usdc_balance"
        val balanceEightState = "Eight"
        val accountBalance = "8"
        val amountAboveBalance = "9"
        val receiveTokenSymbol = "POL"
        val receiveTokenName = "Polygon"
        val feeCoverageTitle = getResourceString(CoreResR.string.send_network_fee_warning_title)

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$balanceScenario' to '$balanceEightState'") {
                setWireMockScenarioState(balanceScenario, balanceEightState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Click on 'Within your portfolio' card") {
                onTangemPayWithdrawHubScreen { withinPortfolioCard.performClick() }
            }
            step("Assert 'Withdraw' Swap screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }
            }
            step("Choose '$receiveTokenName' of '$mainAccountName' as the receive token") {
                chooseMoneyAccountWithdrawReceiveToken(mainAccountName, receiveTokenSymbol, polygon)
            }
            step("Assert source balance contains '$accountBalance'") {
                flakySafely { onSwapTokenScreen { swapCardBalance.assertTextContains(accountBalance, substring = true) } }
            }
            step("Enter withdraw amount '$amountAboveBalance'") { enterWithdrawAmount(amountAboveBalance) }
            step("Assert 'Insufficient funds' error is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { insufficientFundsErrorTitle.assertIsDisplayed() } }
            }
            step("Assert 'Insufficient funds' button is displayed instead of 'Swap'") {
                onSwapTokenScreen { insufficientFundsButton.assertIsDisplayed() }
            }
            step("Click on 'Max' button") { clickSwapMaxAmountButton() }
            step("Assert amount is the account balance '$accountBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { textInput.assertTextEquals(accountBalance) } }
            }
            step("Assert 'Insufficient funds' error is not displayed") {
                onSwapTokenScreen { insufficientFundsErrorTitle.assertDoesNotExist() }
            }
            step("Assert network fee is paid in '$usdc'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { feeBlockCurrency(usdc).assertIsDisplayed() } }
            }
            step("Assert no '$polygon' fee warning is displayed") {
                onSwapTokenScreen { insufficientFeeForTransferNotificationTitle(polygon).assertDoesNotExist() }
            }
            step("Assert '$feeCoverageTitle' warning is displayed: the fee is taken from the amount") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { warningTitle(feeCoverageTitle).assertIsDisplayed() } }
            }
            step("Assert the main action button is enabled") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { mainButtonWithAnyLabel.assertIsEnabled() } }
            }
        }
    }
}