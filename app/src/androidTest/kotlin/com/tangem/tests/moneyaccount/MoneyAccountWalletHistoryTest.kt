package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.openMainWithMoneyAccountWallet
import com.tangem.scenarios.openTokenDetailsFromAccountSection
import com.tangem.scenarios.revealTokenInAccountOnMain
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.screens.onTxHistoryDetailsBottomSheet
import com.tangem.screens.onTxHistoryScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class MoneyAccountWalletHistoryTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val eligibilityState = "PaeraCustomer"
    private val moneyAccountScenario = "tangem_pay_money_account"
    private val moneyAccountActiveState = "AccountActive"
    private val moneyAccountLabel = getResourceString(CoreResR.string.virtual_account_title)
    private val mainAccountName = getResourceString(CoreResR.string.account_main_account_title)
    private val externalAddressStart = "0x4444"

    @AllureId("13369")
    @Ignore("[REDACTED_JIRA]")
    @DisplayName("Wallet history: a transfer to the Money account is labelled 'USD account'")
    @Test
    fun transferToAccountIsLabelledUsdAccountTest() {
        val userTokensUsdcState = "TangemPayTransferUsdc"
        val walletUsdcHistoryScenario = "wallet_usdc_history"
        val walletUsdcHistoryState = "MoneyAccountTransfers"
        val usdc = "USDC"
        val transferredTitle = getResourceString(CoreResR.string.common_transferred)
        val sentTitle = getResourceString(CoreResR.string.common_sent)
        val recipientLabel = getResourceString(CoreResR.string.send_recipient)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensUsdcState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensUsdcState)
            }
            step("Set WireMock scenario '$walletUsdcHistoryScenario' to '$walletUsdcHistoryState'") {
                setWireMockScenarioState(walletUsdcHistoryScenario, walletUsdcHistoryState)
            }
            step("Open 'Main' screen with the Money account wallet") { openMainWithMoneyAccountWallet() }
            step("Reveal '$usdc' in '$mainAccountName'") {
                revealTokenInAccountOnMain(tokenName = usdc, accountName = mainAccountName)
            }
            step("Click on token '$usdc'") { openTokenDetailsFromAccountSection(usdc) }
            step("Assert '$transferredTitle' row shows '$moneyAccountLabel' as the recipient") {
                onTxHistoryScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        transactionItemWithText(transferredTitle, moneyAccountLabel).assertIsDisplayed()
                    }
                }
            }
            step("Click on '$transferredTitle' row") {
                onTxHistoryScreen { transactionItem(transferredTitle).performClick() }
            }
            step("Assert '$recipientLabel' is '$moneyAccountLabel' in the transaction details") {
                onTxHistoryDetailsBottomSheet {
                    flakySafely { valueNextToLabel(recipientLabel, moneyAccountLabel).assertIsDisplayed() }
                }
            }
            step("Click on system 'Back' button") { device.uiDevice.pressBack() }
            step("Assert '$sentTitle' row to the external address shows the address") {
                onTxHistoryScreen {
                    flakySafely { transactionItemWithText(sentTitle, externalAddressStart).assertIsDisplayed() }
                }
            }
        }
    }

    @AllureId("13726")
    @Ignore("[REDACTED_JIRA]")
    @DisplayName("Wallet history: a swap into the Money account USDC is labelled 'USD account'")
    @Test
    fun swapToAccountIsLabelledUsdAccountTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val bitcoinBalanceState = "BalanceHotWalletSvS"
        val exchangeHistoryScenario = "exchange_history"
        val exchangeHistoryState = "SwapToMoneyAccount"
        val bitcoin = "Bitcoin"
        val swappedTitle = getResourceString(CoreResR.string.common_swapped)
        val toLabel = getResourceString(CoreResR.string.common_to)

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(exchangeHistoryScenario, exchangeHistoryState)
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$bitcoinScenario' to '$bitcoinBalanceState'") {
                setWireMockScenarioState(bitcoinScenario, bitcoinBalanceState)
            }
            step("Open 'Main' screen with the Money account wallet") { openMainWithMoneyAccountWallet() }
            step("Reveal '$bitcoin' in '$mainAccountName'") {
                revealTokenInAccountOnMain(tokenName = bitcoin, accountName = mainAccountName)
            }
            step("Click on token '$bitcoin'") { openTokenDetailsFromAccountSection(bitcoin) }
            step("Scroll history to the '$swappedTitle' row with '$moneyAccountLabel'") {
                onTokenDetailsScreen { flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { scrollToText(moneyAccountLabel) } }
            }
            step("Assert '$swappedTitle' row shows '$moneyAccountLabel' as the recipient") {
                onTxHistoryScreen {
                    transactionItemWithText(swappedTitle, moneyAccountLabel).assertIsDisplayed()
                }
            }
            step("Assert '$swappedTitle' row to the external address shows the address") {
                onTokenDetailsScreen { scrollToText(externalAddressStart) }
                onTxHistoryScreen { transactionItemWithText(swappedTitle, externalAddressStart).assertIsDisplayed() }
            }
            step("Click on '$swappedTitle' row with '$moneyAccountLabel'") {
                onTokenDetailsScreen { scrollToText(moneyAccountLabel) }
                onTxHistoryScreen { transactionItemWithText(swappedTitle, moneyAccountLabel).performClick() }
            }
            step("Assert 'To' shows '$moneyAccountLabel' in the transaction details") {
                onTxHistoryDetailsBottomSheet {
                    flakySafely { valueNextToLabel(toLabel, moneyAccountLabel).assertIsDisplayed() }
                }
            }
        }
    }
}