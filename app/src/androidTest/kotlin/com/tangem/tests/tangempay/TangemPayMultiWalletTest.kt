package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.getQuantityString
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.addNewCardWallet
import com.tangem.scenarios.assertMoneyAccountData
import com.tangem.scenarios.assertMoneyAccountRowOfDisplayedWallet
import com.tangem.scenarios.assertTangemPayPaymentAccountData
import com.tangem.scenarios.assertTangemPayTileOfDisplayedWallet
import com.tangem.scenarios.disableMoneyAccountKeyDerivationForCards
import com.tangem.scenarios.disableTangemPayPerWalletCustomers
import com.tangem.scenarios.enableMoneyAccountKeyDerivationForCards
import com.tangem.scenarios.enableTangemPayPerWalletCustomers
import com.tangem.scenarios.openMainScreenWithTangemPayTile
import com.tangem.scenarios.openMoneyAccountFromRowWithBalance
import com.tangem.scenarios.refreshMainUntilMoneyAccountBalance
import com.tangem.scenarios.openTangemPayFromTileWithSubtitle
import com.tangem.scenarios.returnToMainFromMoneyAccount
import com.tangem.scenarios.returnToMainFromTangemPay
import com.tangem.scenarios.switchToNextWallet
import com.tangem.scenarios.switchToPreviousWallet
import com.tangem.screens.onMainScreen
import com.tangem.screens.tangempay.onTangemPayMainScreen
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayMultiWalletTest : BaseTestCase() {

    @AllureId("11642")
    @DisplayName("Tangem Pay: Main, Payment account and Money account show the data of the open wallet")
    @Test
    fun switchingWalletsShowsTangemPayDataOfSelectedWalletTest() {
        val paeraCustomerState = "PaeraCustomer"
        val multiWalletScenario = "tangem_pay_multi_wallet"
        val twoCustomersState = "TwoCustomers"
        val walletACardsSubtitle = getQuantityString(CoreResR.plurals.tangempay_cards_count, 1, 1)
        val walletABalance = "\$10.00"
        val walletACardDigits = "4242"
        val walletATransaction = "Wallet A Shop"
        val walletBCardsSubtitle = getQuantityString(CoreResR.plurals.tangempay_cards_count, 2, 2)
        val walletBBalance = "\$250.00"
        val walletBCardDigits = listOf("4242", "5252")
        val walletBTransaction = "Wallet B Shop"
        val moneyAccountScenario = "tangem_pay_money_account"
        val walletAMoneyAccountBalance = "\$25.00"
        val walletASenderEnd = "a11ce"
        val walletBMoneyAccountBalance = "\$40.00"
        val walletBSenderEnd = "c3be"

        setupHooks(
            additionalBeforeSection = { enableTangemPayPerWalletCustomers() },
            additionalAfterSection = {
                disableTangemPayPerWalletCustomers()
                disableMoneyAccountKeyDerivationForCards()
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$paeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, paeraCustomerState)
            }
            step("Set WireMock scenario '$multiWalletScenario' to '$twoCustomersState'") {
                setWireMockScenarioState(multiWalletScenario, twoCustomersState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$twoCustomersState'") {
                setWireMockScenarioState(moneyAccountScenario, twoCustomersState)
            }
            step("Open Main screen with the Tangem Pay tile of wallet A") { openMainScreenWithTangemPayTile() }
            step("Add card wallet B") { addNewCardWallet(WalletMockContent) }
            step("Enable 'Money account' key derivation for card wallets") { enableMoneyAccountKeyDerivationForCards() }
            step("Switch to wallet A") { switchToPreviousWallet() }
            step("Assert Tangem Pay tile of wallet A is displayed") {
                assertTangemPayTileOfDisplayedWallet(walletACardsSubtitle, walletABalance)
            }
            step("Open Payment account of wallet A") { openTangemPayFromTileWithSubtitle(walletACardsSubtitle) }
            step("Assert Payment account shows the data of wallet A") {
                assertTangemPayPaymentAccountData(
                    fiatBalance = walletABalance,
                    cardDigits = listOf(walletACardDigits),
                    transaction = walletATransaction,
                    otherWalletTransaction = walletBTransaction,
                )
            }
            step("Return to the Main screen") { returnToMainFromTangemPay() }
            step("Assert 'Money account' row of wallet A is displayed") {
                assertMoneyAccountRowOfDisplayedWallet(walletAMoneyAccountBalance)
            }
            step("Open 'Money account' of wallet A") { openMoneyAccountFromRowWithBalance(walletAMoneyAccountBalance) }
            step("Assert 'Money account' shows the data of wallet A") {
                assertMoneyAccountData(
                    fiatBalance = walletAMoneyAccountBalance,
                    senderEnd = walletASenderEnd,
                    otherWalletSenderEnd = walletBSenderEnd,
                )
            }
            step("Return to the Main screen") { returnToMainFromMoneyAccount() }
            step("Switch to wallet B") { switchToNextWallet() }
            step("Assert Tangem Pay tile of wallet B is displayed") {
                assertTangemPayTileOfDisplayedWallet(walletBCardsSubtitle, walletBBalance)
            }
            step("Refresh 'Main' until the 'Money account' row of wallet B shows its balance") {
                refreshMainUntilMoneyAccountBalance(walletBMoneyAccountBalance, isMultiWallet = true)
            }
            step("Open Payment account of wallet B") { openTangemPayFromTileWithSubtitle(walletBCardsSubtitle) }
            step("Assert Payment account shows the data of wallet B") {
                assertTangemPayPaymentAccountData(
                    fiatBalance = walletBBalance,
                    cardDigits = walletBCardDigits,
                    transaction = walletBTransaction,
                    otherWalletTransaction = walletATransaction,
                )
            }
            step("Return to the Main screen") { returnToMainFromTangemPay() }
            step("Open 'Money account' of wallet B") { openMoneyAccountFromRowWithBalance(walletBMoneyAccountBalance) }
            step("Assert 'Money account' shows the data of wallet B") {
                assertMoneyAccountData(
                    fiatBalance = walletBMoneyAccountBalance,
                    senderEnd = walletBSenderEnd,
                    otherWalletSenderEnd = walletASenderEnd,
                )
            }
        }
    }

    @AllureId("9688")
    @DisplayName("Tangem Pay: adding a wallet keeps the wallet with the Payment account")
    @Test
    fun addingWalletKeepsWalletWithPaymentAccountTest() {
        val hotWalletCustomerOnlyState = "HotWalletCustomerOnly"
        val hotWalletName = "Wallet"
        val cardWalletName = "Wallet 2"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$hotWalletCustomerOnlyState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, hotWalletCustomerOnlyState)
            }
            step("Open Main screen with the Tangem Pay tile of the hot wallet") { openMainScreenWithTangemPayTile() }
            step("Add card wallet") { addNewCardWallet(WalletMockContent) }
            step("Assert '$cardWalletName' wallet is displayed") {
                flakySafely { onMainScreen { walletNameText(cardWalletName).assertIsDisplayed() } }
            }
            step("Switch to '$hotWalletName' wallet") { switchToPreviousWallet() }
            step("Assert '$hotWalletName' wallet is displayed") {
                flakySafely { onMainScreen { walletNameText(hotWalletName).assertIsDisplayed() } }
            }
            step("Assert Tangem Pay tile is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { mainScreenTile.assertIsDisplayed() }
                }
            }
        }
    }
}