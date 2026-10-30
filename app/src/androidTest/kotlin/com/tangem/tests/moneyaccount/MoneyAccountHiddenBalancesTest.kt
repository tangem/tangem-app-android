package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankAccounts.JOHN
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.hideBalances
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.showBalances
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.moneyaccount.onMoneyAccountAddFundsSheet
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawAmountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawPreviewScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import com.tangem.utils.StringsSigns.THREE_STARS
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class MoneyAccountHiddenBalancesTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val eligibilityState = "PaeraCustomer"
    private val moneyAccountScenario = "tangem_pay_money_account"
    private val moneyAccountActiveState = "AccountActive"
    private val usdcHistoryScenario = "money_account_usdc_history"
    private val usdcHistoryOutgoingState = "Outgoing"
    private val userTokensUsdcState = "TangemPayTransferUsdc"
    private val bankOfframpChannelsState = "BankOfframp"
    private val storiesScenario = "stories_first_time_swap_v2"
    private val storiesErrorState = "Error"

    private val initialBalance = "$25.00"
    private val sentAmount = "3.00"
    private val usdc = "USDC"
    private val polygon = "Polygon"
    private val sentTitle = getResourceString(CoreResR.string.common_sent)
    private val transferredTitle = getResourceString(CoreResR.string.common_transferred)
    private val swappedTitle = getResourceString(CoreResR.string.common_swapped)
    private val mainAccountName = getResourceString(CoreResR.string.account_main_account_title)

    @AllureId("13455")
    @DisplayName("Money account: hidden balances mask the account row and the wallet total on Main")
    @Test
    fun mainRowAndWalletBalanceMaskedTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Main' screen with the Money account wallet") { openMainWithMoneyAccountWallet() }
            step("Assert wallet balance is '$THREE_STARS'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onMainScreen { totalBalanceText.assertTextEquals(THREE_STARS) } }
            }
            step("Assert 'Money account' row balance is '$THREE_STARS'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onMoneyAccountScreen { mainRowWithText(THREE_STARS).assertIsDisplayed() }
                }
            }
        }
    }

    @Ignore("[REDACTED_JIRA]")
    @AllureId("13456")
    @DisplayName("Money account: hidden balances mask the account screen header as on Main")
    @Test
    fun accountHeaderBalanceMaskedTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert account balance is '$THREE_STARS'") {
                flakySafely { onMoneyAccountScreen { balance.assertTextEquals(THREE_STARS) } }
            }
        }
    }

    @AllureId("13457")
    @DisplayName("Money account: hidden balances mask Sent, Transferred and Swapped amounts in the history")
    @Test
    fun historyOperationAmountsMaskedTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryOutgoingState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryOutgoingState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Refresh 'Money account' screen until the history loads") {
                refreshMoneyAccountUntilTransaction(sentTitle)
            }
            listOf(sentTitle, transferredTitle, swappedTitle).forEach { title ->
                step("Assert '$title' amount is '$THREE_STARS'") {
                    flakySafely { onTxHistoryScreen { transactionAmount(title).assertTextEquals(THREE_STARS) } }
                }
            }
        }
    }

    @AllureId("13458")
    @DisplayName("Money account: hidden balances mask the amount in the operation details")
    @Test
    fun operationDetailsAmountMaskedTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryOutgoingState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryOutgoingState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Refresh 'Money account' screen until the history loads") {
                refreshMoneyAccountUntilTransaction(sentTitle)
            }
            step("Assert '$transferredTitle' transaction is displayed") {
                flakySafely { onTxHistoryScreen { transactionItem(transferredTitle).assertIsDisplayed() } }
            }
            step("Click on '$sentTitle' transaction") {
                onTxHistoryScreen { transactionItem(sentTitle).performClick() }
            }
            step("Assert '$sentTitle' operation details are displayed") {
                flakySafely { onTxHistoryDetailsBottomSheet { headerTitle(sentTitle).assertIsDisplayed() } }
            }
            step("Assert operation amount is '$THREE_STARS'") {
                onTxHistoryDetailsBottomSheet { amount.assertTextEquals(THREE_STARS) }
            }
        }
    }

    @AllureId("13459")
    @DisplayName("Money account: hidden balances mask both balances in Add funds from Tangem Wallet")
    @Test
    fun addFundsFromTangemWalletBalancesMaskedTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            step("Click on 'From your Tangem Wallet' option") {
                onMoneyAccountAddFundsSheet { swapOption.performClick() }
            }
            step("Assert 'Add funds' Swap screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { addFundsTitle.assertIsDisplayed() } }
            }
            step("Assert source balance is '$THREE_STARS'") {
                flakySafely { onSwapTokenScreen { swapCardBalance.assertTextEquals(THREE_STARS) } }
            }
            step("Assert 'You receive in USD account' balance is '$THREE_STARS'") {
                onSwapTokenScreen { receiveCardBalance.assertTextEquals(THREE_STARS) }
            }
        }
    }

    @AllureId("13460")
    @DisplayName("Money account: hidden balances mask the balances in Send within the portfolio")
    @Test
    fun sendWithinPortfolioBalancesMaskedTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Click on 'Within your portfolio' card") {
                onTangemPayWithdrawHubScreen { withinPortfolioCard.performClick() }
            }
            step("Assert 'Withdraw' Swap screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }
            }
            step("Assert account balance is '$THREE_STARS'") {
                flakySafely { onSwapTokenScreen { swapCardBalance.assertTextEquals(THREE_STARS) } }
            }
            step("Click on 'Choose token' button in the 'To' block") {
                onSwapTokenScreen { receiveChooseTokenButton.performClick() }
            }
            step("Expand account '$mainAccountName' in receive selector") {
                flakySafely { onSwapSelectTokenScreen { tokenWithName(mainAccountName).performClick() } }
            }
            step("Assert '$usdc' on '$polygon' amount is '$THREE_STARS'") {
                flakySafely {
                    onSwapSelectTokenScreen {
                        tokenWithNameNetworkAndAmount(usdc, polygon, THREE_STARS).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("13461")
    @DisplayName("Money account: hidden balances mask the source balance of the bank transfer")
    @Test
    fun bankTransferSourceBalanceMaskedTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$bankOfframpChannelsState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, bankOfframpChannelsState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert '$usdc' on '$polygon' source balance is '$THREE_STARS'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen {
                        sourceRowWithBalance(usdc, polygon, THREE_STARS).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("13462")
    @DisplayName("Money account: hidden balances mask the balance on the bank transfer confirmation")
    @Test
    fun bankTransferConfirmationBalanceMaskedTest() {
        val bankAccountsScenario = "tangem_pay_bank_accounts"
        val twoAccountsState = "TwoAccounts"
        val amount = "5"

        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$bankOfframpChannelsState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, bankOfframpChannelsState)
            }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Open recent recipient '${JOHN.shortName}'") { openTangemPayRecentRecipient(JOHN) }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert 'Confirm transfer' screen is displayed") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { title.assertIsDisplayed() } }
            }
            step("Assert 'From USD account' balance is '$THREE_STARS'") {
                flakySafely {
                    onTangemPayBankWithdrawPreviewScreen { amountCard("$$amount", THREE_STARS).assertIsDisplayed() }
                }
            }
        }
    }

    @Ignore("[REDACTED_JIRA]")
    @AllureId("13463")
    @DisplayName("Money account: unhiding balances on the open account screen reveals the header and history")
    @Test
    fun unhideOnOpenAccountRevealsAmountsTest() {
        setupHooks(
            additionalPreferencesSection = { appPreferencesStore.hideBalances(it) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryOutgoingState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryOutgoingState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Refresh 'Money account' screen until the history loads") {
                refreshMoneyAccountUntilTransaction(sentTitle)
            }
            step("Assert '$sentTitle' amount is '$THREE_STARS'") {
                flakySafely { onTxHistoryScreen { transactionAmount(sentTitle).assertTextEquals(THREE_STARS) } }
            }
            step("Assert account balance is '$THREE_STARS'") {
                onMoneyAccountScreen { balance.assertTextEquals(THREE_STARS) }
            }
            step("Show balances") { appPreferencesStore.showBalances() }
            step("Assert account balance is '$initialBalance'") {
                flakySafely { onMoneyAccountScreen { balance.assertTextEquals(initialBalance) } }
            }
            step("Assert '$sentTitle' amount contains '$sentAmount'") {
                flakySafely {
                    onTxHistoryScreen { transactionAmount(sentTitle).assertTextContains(sentAmount, substring = true) }
                }
            }
        }
    }
}