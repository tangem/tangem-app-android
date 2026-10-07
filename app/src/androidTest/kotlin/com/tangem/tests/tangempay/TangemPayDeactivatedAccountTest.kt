package com.tangem.tests.tangempay

import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayDeactivatedAccountTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val removalScenario = "tangem_pay_account_removal_android"

    @AllureId("9714")
    @DisplayName("Tangem Pay: the Main tile of a deactivated account shows 'Account closed'")
    @Test
    fun mainTileShowsAccountClosedWhenAccountIsDeactivatedTest() {
        val deactivatedState = "Deactivated"
        val accountClosedStatus = getResourceString(CoreResR.string.tangempay_status_deactivated)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$removalScenario' to '$deactivatedState'") {
                setWireMockScenarioState(removalScenario, deactivatedState)
            }
            step("Open 'Main' screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Assert Tangem Pay tile with '$accountClosedStatus' status is displayed") {
                awaitSuccess { onTangemPayMainScreen { tileWithSubtitle(accountClosedStatus).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("9715")
    @DisplayName("Tangem Pay: the Payment account of a deactivated account shows the account-closed block")
    @Test
    fun paymentAccountShowsAccountClosedBlockWhenAccountIsDeactivatedTest() {
        val deactivatedState = "Deactivated"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$removalScenario' to '$deactivatedState'") {
                setWireMockScenarioState(removalScenario, deactivatedState)
            }
            step("Open the deactivated Tangem Pay payment account") { openDeactivatedTangemPay() }
            step("Assert account-closed banner icon is displayed") {
                onTangemPayMainScreen { accountClosedBannerIcon.assertIsDisplayed() }
            }
            step("Assert account-closed banner message is displayed") {
                onTangemPayMainScreen { accountClosedBannerMessage.assertIsDisplayed() }
            }
            step("Assert 'Remove account' button is displayed") {
                onTangemPayMainScreen { removeAccountBannerButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("9719")
    @DisplayName("Tangem Pay: a deactivated account is topped up through Swap")
    @Test
    fun topUpViaSwapWhenAccountIsDeactivatedTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val expressAssetsScenario = "express_api_assets"
        val historyScenario = "tangem_pay_transaction_history"
        // The plain 'Deactivated' fixture has no networks, and Android disables Add funds without one.
        val deactivatedWithNetworksState = "DeactivatedWithNetworks"
        val bitcoinBalanceState = "BalanceHotWalletSvS"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val historyState = "InitialEmpty"
        val bitcoinSymbol = "BTC"
        val swapFromAmount = "0.001"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$removalScenario' to '$deactivatedWithNetworksState'") {
                setWireMockScenarioState(removalScenario, deactivatedWithNetworksState)
            }
            step("Set WireMock scenario '$bitcoinScenario' to '$bitcoinBalanceState'") {
                setWireMockScenarioState(bitcoinScenario, bitcoinBalanceState)
            }
            step("Set WireMock scenario '$expressAssetsScenario' to '$expressAssetsState'") {
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
            }
            step("Set WireMock scenario '$historyScenario' to '$historyState'") {
                setWireMockScenarioState(historyScenario, historyState)
            }
            step("Open the deactivated Tangem Pay payment account") { openDeactivatedTangemPay() }
            step("Click on 'Add funds' button") {
                onTangemPayMainScreen { topUpButton.clickWithAssertion() }
            }
            step("Assert 'Add funds' sheet is displayed") {
                awaitSuccess { onTangemPayAddFundsSheet { title.assertIsDisplayed() } }
            }
            step("Click on 'Swap' option") {
                onTangemPayAddFundsSheet { swapOption.clickWithAssertion() }
            }
            step("Click on 'Close' button on Swap stories") {
                onSwapStoriesScreen { closeButton.performClick() }
            }
            step("Assert 'Add funds' screen is displayed") {
                awaitSuccess { onSwapTokenScreen { addFundsTitle.assertIsDisplayed() } }
            }
            // Android fills FROM with the wallet's most-funded token itself, so the case's pick is an assertion.
            step("Assert '$bitcoinSymbol' is pre-selected as the source token") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(bitcoinSymbol).assertIsDisplayed() }
                }
            }
            step("Enter swap amount '$swapFromAmount'") {
                onSwapTokenScreen {
                    textInput.performClick()
                    textInput.performTextReplacement(swapFromAmount)
                }
            }
            step("Dismiss keyboard") { Espresso.closeSoftKeyboard() }
            step("Wait until provider quote and fee are loaded") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        networkFeeBlock.assertIsDisplayed()
                        feeAmount.assertIsDisplayed()
                    }
                }
            }
            step("Confirm swap by holding the button") {
                confirmSwapByHolding(accessCode = TANGEM_PAY_ACCESS_CODE)
            }
            step("Assert 'Swap in progress' screen is displayed") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Assert account-closed banner is displayed") {
                awaitSuccess { onTangemPayMainScreen { accountClosedBannerTitle.assertIsDisplayed() } }
            }
        }
    }
}