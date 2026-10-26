package com.tangem.tests.tangempay

import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.tapBackButton
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import java.math.BigDecimal

@HiltAndroidTest
class TangemPayCrossWalletSwapTest : BaseTestCase() {

    private val storiesScenario = "stories_first_time_swap_v2"
    private val storiesErrorState = "Error"
    private val balanceScenario = "tangem_pay_balance_update"
    private val tetherBalanceScenario = "ethereum_usdt_balance"

    private val hotWalletName = "Wallet"
    private val cardWalletName = "Wallet 2"
    private val mainAccountName = "Main account"
    private val tether = "Tether"
    private val tetherSymbol = "USDT"
    private val usdcSymbol = "USDC"
    private val initialPayBalance = "10.00"

    @AllureId("9680")
    @DisplayName("Tangem Pay: swap from a token of one wallet to the Payment account of another wallet")
    @Test
    fun swapFromTokenToPaymentAccountOfOtherWalletTest() {
        val depositAmount = "100"
        val sentDepositAmount = "100.00"
        val payBalanceAfterDepositState = "AfterDeposit"
        val tetherAfterDepositState = "AfterPayDeposit"
        val payBalanceAfterDeposit = "110.00"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenarios for a cross-wallet Tangem Pay swap") { setTangemPayCrossWalletSwapScenarios() }
            step("Open 'Main' screen with existing hot wallet") { openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12) }
            step("Generate missing addresses") { generateMissingHotWalletAddresses() }
            step("Wait for addresses to be generated") { waitForAddressesGenerated() }
            step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
            step("Reveal '$tether' in '$mainAccountName' of '$hotWalletName'") {
                revealTokenInAccountOnMain(tokenName = tether, accountName = mainAccountName)
            }
            var tetherBalanceBefore = BigDecimal.ZERO
            step("Read '$tether' balance") { tetherBalanceBefore = readDisplayedTokenBalance(tether) }
            step("Add card wallet '$cardWalletName'") { addNewCardWallet(WalletMockContent) }
            step("Open Payment account of '$cardWalletName'") { openTangemPayOfDisplayedWallet() }
            step("Assert payment account balance contains '$initialPayBalance'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(initialPayBalance, substring = true) }
                }
            }
            step("Open 'Add funds' sheet") { openTangemPayAddFunds() }
            step("Click on 'From your Tangem Wallet' option") {
                onTangemPayAddFundsSheet { swapOption.performClick() }
            }
            step("Assert 'Add funds' Swap screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { addFundsTitle.assertIsDisplayed() } }
            }
            step("Click on 'Select token' icon in 'You send' block") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { sourceSelectTokenIcon.assertIsDisplayed() } }
                onSwapTokenScreen { sourceSelectTokenIcon.performClick() }
            }
            step("Choose '$tether' of '$hotWalletName' as source token") {
                selectTokenInAccountOnWalletTab(
                    walletName = hotWalletName,
                    accountName = mainAccountName,
                    tokenName = tether,
                )
            }
            step("Assert '$tetherSymbol' is the source token") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onSwapTokenScreen { swapTokenSymbol(tetherSymbol).assertIsDisplayed() }
                }
            }
            step("Enter amount '$depositAmount'") { inputAmount(depositAmount) }
            step("Dismiss keyboard") {
                Espresso.closeSoftKeyboard()
                waitForIdle()
            }
            step("Assert network fee is displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        networkFeeBlock.assertIsDisplayed()
                        feeAmount.assertIsDisplayed()
                    }
                }
            }
            step("Confirm swap by holding the button") { confirmSwapByHolding() }
            step("Assert 'Swap in progress' screen is displayed") {
                flakySafely { onSwapSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Assert 'You send' block shows '$sentDepositAmount $tetherSymbol' from '$mainAccountName'") {
                onSwapSuccessScreen {
                    sendBlockText(mainAccountName).assertIsDisplayed()
                    sendBlockText(tetherSymbol).assertTextContains(sentDepositAmount, substring = true)
                }
            }
            step("Assert 'You receive' block shows '$usdcSymbol' in '$PAY_ACCOUNT_ROW_NAME'") {
                onSwapSuccessScreen {
                    receiveBlockText(PAY_ACCOUNT_ROW_NAME).assertIsDisplayed()
                    receiveBlockText(usdcSymbol).assertIsDisplayed()
                }
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Switch WireMock scenario '$balanceScenario' to '$payBalanceAfterDepositState'") {
                setWireMockScenarioState(balanceScenario, payBalanceAfterDepositState)
            }
            step("Switch WireMock scenario '$tetherBalanceScenario' to '$tetherAfterDepositState'") {
                setWireMockScenarioState(tetherBalanceScenario, tetherAfterDepositState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert payment account balance contains '$payBalanceAfterDeposit'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(payBalanceAfterDeposit, substring = true) }
                }
            }
            step("Return to the Main screen") { returnToMainFromTangemPay() }
            step("Switch to '$hotWalletName'") { switchToPreviousWallet() }
            step("Assert '$tether' balance of '$hotWalletName' decreased") {
                refreshMainUntilTokenBalance(tether, "decreases below $tetherBalanceBefore") {
                    it < tetherBalanceBefore
                }
            }
        }
    }

    @AllureId("9683")
    @DisplayName("Tangem Pay: swap from the Payment account to a token of another wallet via Withdraw")
    @Test
    fun swapFromPaymentAccountToTokenOfOtherWalletViaWithdrawTest() {
        val withdrawAmount = "5"
        val sentWithdrawAmount = "5.00"
        val channelsBankOfframpState = "BankOfframp"
        val payBalanceAfterWithdrawState = "AfterWithdraw"
        val tetherAfterWithdrawState = "AfterPayWithdraw"
        val payBalanceAfterWithdraw = "5.00"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenarios for a cross-wallet Tangem Pay swap") { setTangemPayCrossWalletSwapScenarios() }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBankOfframpState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBankOfframpState)
            }
            step("Open 'Main' screen with existing hot wallet") { openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12) }
            step("Generate missing addresses") { generateMissingHotWalletAddresses() }
            step("Wait for addresses to be generated") { waitForAddressesGenerated() }
            step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
            step("Add card wallet '$cardWalletName'") { addNewCardWallet(WalletMockContent) }
            step("Reveal '$tether' in '$mainAccountName' of '$cardWalletName'") {
                revealTokenInAccountOnMain(tokenName = tether, accountName = mainAccountName)
            }
            var tetherBalanceBefore = BigDecimal.ZERO
            step("Read '$tether' balance") { tetherBalanceBefore = readDisplayedTokenBalance(tether) }
            step("Switch to '$hotWalletName'") { switchToPreviousWallet() }
            step("Open Payment account of '$hotWalletName'") { openTangemPayOfDisplayedWallet() }
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
            step("Choose '$tether' of '$cardWalletName' as receive token") {
                selectTokenInAccountOnWalletTab(
                    walletName = cardWalletName,
                    accountName = mainAccountName,
                    tokenName = tether,
                )
            }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Assert receive amount is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { receiveAmount.assertIsDisplayed() } }
            }
            step("Confirm swap by holding the button") { confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE) }
            step("Assert 'Swap in progress' screen is displayed") {
                flakySafely { onSwapSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Assert 'You send' block shows '$sentWithdrawAmount $usdcSymbol' from '$PAY_ACCOUNT_ROW_NAME'") {
                onSwapSuccessScreen {
                    sendBlockText(PAY_ACCOUNT_ROW_NAME).assertIsDisplayed()
                    sendBlockText(usdcSymbol).assertTextContains(sentWithdrawAmount, substring = true)
                }
            }
            step("Assert 'You receive' block shows '$tetherSymbol' in '$mainAccountName'") {
                onSwapSuccessScreen {
                    receiveBlockText(mainAccountName).assertIsDisplayed()
                    receiveBlockText(tetherSymbol).assertIsDisplayed()
                }
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Assert 'Withdraw' hub screen is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
            }
            step("Return to Payment account") { returnToPaymentAccountFromWithdrawHub() }
            step("Switch WireMock scenario '$balanceScenario' to '$payBalanceAfterWithdrawState'") {
                setWireMockScenarioState(balanceScenario, payBalanceAfterWithdrawState)
            }
            step("Switch WireMock scenario '$tetherBalanceScenario' to '$tetherAfterWithdrawState'") {
                setWireMockScenarioState(tetherBalanceScenario, tetherAfterWithdrawState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert payment account balance contains '$payBalanceAfterWithdraw'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(payBalanceAfterWithdraw, substring = true) }
                }
            }
            step("Return to the Main screen") { returnToMainFromTangemPay() }
            step("Switch to '$cardWalletName'") { switchToNextWallet() }
            step("Assert '$tether' balance of '$cardWalletName' increased") {
                refreshMainUntilTokenBalance(tether, "increases above $tetherBalanceBefore") {
                    it > tetherBalanceBefore
                }
            }
        }
    }

    @AllureId("12128")
    @DisplayName("Tangem Pay: swap from the Payment account to a token of another wallet started from the token")
    @Test
    fun swapFromPaymentAccountToTokenOfOtherWalletFromTokenTest() {
        val withdrawAmount = "5"
        val sentWithdrawAmount = "5.00"
        val payBalanceAfterWithdrawState = "AfterWithdraw"
        val tetherAfterWithdrawState = "AfterPayWithdraw"
        val payBalanceAfterWithdraw = "5.00"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenarios for a cross-wallet Tangem Pay swap") { setTangemPayCrossWalletSwapScenarios() }
            step("Open 'Main' screen with existing hot wallet") { openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12) }
            step("Generate missing addresses") { generateMissingHotWalletAddresses() }
            step("Wait for addresses to be generated") { waitForAddressesGenerated() }
            step("Close 'Finalize wallet setup' sheet if shown") { closeFinishWalletActivationSheetIfShown() }
            step("Add card wallet '$cardWalletName'") { addNewCardWallet(WalletMockContent) }
            step("Reveal '$tether' in '$mainAccountName' of '$cardWalletName'") {
                revealTokenInAccountOnMain(tokenName = tether, accountName = mainAccountName)
            }
            var tetherBalanceBefore = BigDecimal.ZERO
            step("Read '$tether' balance") { tetherBalanceBefore = readDisplayedTokenBalance(tether) }
            step("Click on token '$tether'") { openTokenDetailsFromAccountSection(tether) }
            step("Open 'Swap' screen") { openSwapScreen(from = SwapEntryPoint.TokenDetails, storiesExist = false) }
            step("Open source token selector") { openSwapFromTokenSelector() }
            step("Choose '$PAY_ACCOUNT_ROW_NAME' of '$hotWalletName' as source") {
                selectTokenInAccountOnWalletTab(
                    walletName = hotWalletName,
                    accountName = PAY_ACCOUNT_ROW_NAME,
                    tokenName = usdcSymbol,
                )
            }
            step("Click on 'Choose token' button") { onSwapTokenScreen { chooseTokenButton.performClick() } }
            step("Choose '$tether' of '$cardWalletName' as receive token") {
                selectTokenInAccountOnWalletTab(
                    walletName = cardWalletName,
                    accountName = mainAccountName,
                    tokenName = tether,
                )
            }
            step("Enter withdraw amount '$withdrawAmount'") { enterWithdrawAmount(withdrawAmount) }
            step("Assert receive amount is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { receiveAmount.assertIsDisplayed() } }
            }
            step("Confirm swap by holding the button") { confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE) }
            step("Assert 'Swap in progress' screen is displayed") {
                flakySafely { onSwapSuccessScreen { title.assertIsDisplayed() } }
            }
            step("Assert 'You send' block shows '$sentWithdrawAmount $usdcSymbol' from '$PAY_ACCOUNT_ROW_NAME'") {
                onSwapSuccessScreen {
                    sendBlockText(PAY_ACCOUNT_ROW_NAME).assertIsDisplayed()
                    sendBlockText(usdcSymbol).assertTextContains(sentWithdrawAmount, substring = true)
                }
            }
            step("Assert 'You receive' block shows '$tetherSymbol' in '$mainAccountName'") {
                onSwapSuccessScreen {
                    receiveBlockText(mainAccountName).assertIsDisplayed()
                    receiveBlockText(tetherSymbol).assertIsDisplayed()
                }
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Switch WireMock scenario '$balanceScenario' to '$payBalanceAfterWithdrawState'") {
                setWireMockScenarioState(balanceScenario, payBalanceAfterWithdrawState)
            }
            step("Switch WireMock scenario '$tetherBalanceScenario' to '$tetherAfterWithdrawState'") {
                setWireMockScenarioState(tetherBalanceScenario, tetherAfterWithdrawState)
            }
            step("Click on 'Back' button") { tapBackButton() }
            step("Assert '$tether' balance of '$cardWalletName' increased") {
                refreshMainUntilTokenBalance(tether, "increases above $tetherBalanceBefore") {
                    it > tetherBalanceBefore
                }
            }
            step("Switch to '$hotWalletName'") { switchToPreviousWallet() }
            step("Open Payment account of '$hotWalletName'") { openTangemPayOfDisplayedWallet() }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert payment account balance contains '$payBalanceAfterWithdraw'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(payBalanceAfterWithdraw, substring = true) }
                }
            }
        }
    }
}