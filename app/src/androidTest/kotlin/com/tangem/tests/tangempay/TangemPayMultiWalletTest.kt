package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.extensions.getQuantityString
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.addNewCardWallet
import com.tangem.scenarios.assertTangemPayPaymentAccountData
import com.tangem.scenarios.assertTangemPayTileOfDisplayedWallet
import com.tangem.scenarios.disableTangemPayPerWalletCustomers
import com.tangem.scenarios.enableTangemPayPerWalletCustomers
import com.tangem.scenarios.openMainScreenWithTangemPayTile
import com.tangem.scenarios.openTangemPayFromTileWithSubtitle
import com.tangem.scenarios.returnToMainFromTangemPay
import com.tangem.scenarios.switchToNextWallet
import com.tangem.scenarios.switchToPreviousWallet
import com.tangem.tap.domain.sdk.mocks.content.WalletMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayMultiWalletTest : BaseTestCase() {

    @AllureId("11642")
    @DisplayName("Tangem Pay: Main tile and Payment account show the Tangem Pay of the open wallet")
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

        setupHooks(
            additionalBeforeSection = { enableTangemPayPerWalletCustomers() },
            additionalAfterSection = { disableTangemPayPerWalletCustomers() },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$paeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, paeraCustomerState)
            }
            step("Set WireMock scenario '$multiWalletScenario' to '$twoCustomersState'") {
                setWireMockScenarioState(multiWalletScenario, twoCustomersState)
            }
            step("Open Main screen with the Tangem Pay tile of wallet A") { openMainScreenWithTangemPayTile() }
            step("Add card wallet B") { addNewCardWallet(WalletMockContent) }
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
            step("Switch to wallet B") { switchToNextWallet() }
            step("Assert Tangem Pay tile of wallet B is displayed") {
                assertTangemPayTileOfDisplayedWallet(walletBCardsSubtitle, walletBBalance)
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
        }
    }
}