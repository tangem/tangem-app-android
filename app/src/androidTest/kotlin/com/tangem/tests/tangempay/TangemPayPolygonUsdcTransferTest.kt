package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.tapBackButton
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import java.math.BigDecimal

@HiltAndroidTest
class TangemPayPolygonUsdcTransferTest : BaseTestCase() {

    private val storiesScenario = "stories_first_time_swap_v2"
    private val storiesErrorState = "Error"
    private val eligibilityState = "PaeraCustomer"
    private val userTokensUsdcState = "TangemPayTransferUsdc"
    private val balanceScenario = "tangem_pay_balance_update"
    private val balanceInitialState = "InitialBalance"
    private val historyScenario = "tangem_pay_transaction_history"
    private val historyEmptyState = "InitialEmpty"
    private val usdcBalanceScenario = "polygon_usdc_balance"

    private val mainAccountName = "Main account"
    private val usdc = "USDC"
    private val initialPayBalance = "10.00"

    @AllureId("9693")
    @DisplayName("Tangem Pay: Polygon USDC is transferred to the Payment account without a provider fee")
    @Test
    fun polygonUsdcTransferToPaymentAccountTest() {
        val depositAmount = "10"
        val payBalanceAfterDepositState = "AfterPolygonDeposit"
        val usdcAfterDepositState = "AfterPayDeposit"
        val payBalanceAfterDeposit = "20.00"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$balanceScenario' to '$balanceInitialState'") {
                setWireMockScenarioState(balanceScenario, balanceInitialState)
            }
            step("Set WireMock scenario '$historyScenario' to '$historyEmptyState'") {
                setWireMockScenarioState(historyScenario, historyEmptyState)
            }
            step("Open 'Main' screen with existing hot wallet") { openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12) }
            step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
            step("Reveal '$usdc' in '$mainAccountName'") {
                revealTokenInAccountOnMain(tokenName = usdc, accountName = mainAccountName)
            }
            var usdcBalanceBefore = BigDecimal.ZERO
            step("Read '$usdc' balance") { usdcBalanceBefore = readDisplayedTokenBalance(usdc) }
            step("Click on token '$usdc'") { openTokenDetailsFromAccountSection(usdc) }
            step("Open 'Swap' screen") { openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false) }
            step("Choose '$usdc' of '$PAY_ACCOUNT_ROW_NAME' as receive token") {
                chooseIdenticalReceiveToken(tokenName = usdc, receiveAccountName = PAY_ACCOUNT_ROW_NAME)
            }
            step("Enter amount '$depositAmount'") { inputAmount(depositAmount) }
            step("Assert Transfer mode is ready without a provider") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
            step("Hold to confirm the transfer") { holdToConfirmTransfer() }
            step("Assert 'Transfer in progress' screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapSuccessScreen { transferInProgressTitle.assertIsDisplayed() } }
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Switch WireMock scenario '$balanceScenario' to '$payBalanceAfterDepositState'") {
                setWireMockScenarioState(balanceScenario, payBalanceAfterDepositState)
            }
            step("Switch WireMock scenario '$usdcBalanceScenario' to '$usdcAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcAfterDepositState)
            }
            step("Click on 'Back' button") { tapBackButton() }
            step("Assert '$usdc' balance decreased") {
                refreshMainUntilTokenBalance(usdc, "decreases below $usdcBalanceBefore") { it < usdcBalanceBefore }
            }
            step("Open Payment account") { openTangemPayOfDisplayedWallet() }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert payment account balance contains '$payBalanceAfterDeposit'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(payBalanceAfterDeposit, substring = true) }
                }
            }
        }
    }

    @AllureId("12129")
    @DisplayName("Tangem Pay: the Payment account transfers to Polygon USDC without a network fee")
    @Test
    fun paymentAccountTransferToPolygonUsdcTest() {
        val withdrawAmount = "5"
        val channelsBankOfframpState = "BankOfframp"
        val payBalanceAfterWithdrawState = "AfterWithdraw"
        val usdcAfterWithdrawState = "AfterPayWithdraw"
        val payBalanceAfterWithdraw = "5.00"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBankOfframpState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBankOfframpState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$balanceScenario' to '$balanceInitialState'") {
                setWireMockScenarioState(balanceScenario, balanceInitialState)
            }
            step("Set WireMock scenario '$historyScenario' to '$historyEmptyState'") {
                setWireMockScenarioState(historyScenario, historyEmptyState)
            }
            step("Open 'Main' screen with existing hot wallet") { openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12) }
            step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
            step("Reveal '$usdc' in '$mainAccountName'") {
                revealTokenInAccountOnMain(tokenName = usdc, accountName = mainAccountName)
            }
            var usdcBalanceBefore = BigDecimal.ZERO
            step("Read '$usdc' balance") { usdcBalanceBefore = readDisplayedTokenBalance(usdc) }
            step("Open Payment account") { openTangemPayOfDisplayedWallet() }
            step("Assert payment account balance contains '$initialPayBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(initialPayBalance, substring = true) }
                }
            }
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Click on 'Within your portfolio' card") {
                onTangemPayWithdrawHubScreen { withinPortfolioCard.performClick() }
            }
            step("Assert 'Withdraw' Swap screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }
            }
            step("Click on 'Choose token' button") { onSwapTokenScreen { chooseTokenButton.performClick() } }
            step("Choose '$usdc' of '$mainAccountName' as receive token") {
                selectTokenInAccount(accountName = mainAccountName, tokenName = usdc)
            }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Assert Transfer mode is ready without a provider") {
                flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { onSwapTokenScreen { transferButtonText.assertIsDisplayed() } }
                onSwapTokenScreen { providersBlock.assertIsNotDisplayed() }
            }
            step("Assert network fee is not displayed") {
                onSwapTokenScreen { networkFeeBlock.assertDoesNotExist() }
            }
            step("Confirm the transfer by holding the button") {
                confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert 'Transfer in progress' screen is displayed") {
                flakySafely { onSwapSuccessScreen { transferInProgressTitle.assertIsDisplayed() } }
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Assert 'Withdraw' hub screen is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
            }
            step("Return to Payment account") { returnToPaymentAccountFromWithdrawHub() }
            step("Switch WireMock scenario '$balanceScenario' to '$payBalanceAfterWithdrawState'") {
                setWireMockScenarioState(balanceScenario, payBalanceAfterWithdrawState)
            }
            step("Switch WireMock scenario '$usdcBalanceScenario' to '$usdcAfterWithdrawState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcAfterWithdrawState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert payment account balance contains '$payBalanceAfterWithdraw'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(payBalanceAfterWithdraw, substring = true) }
                }
            }
            step("Return to the Main screen") { returnToMainFromTangemPay() }
            step("Assert '$usdc' balance increased") {
                refreshMainUntilTokenBalance(usdc, "increases above $usdcBalanceBefore") { it > usdcBalanceBefore }
            }
        }
    }
}