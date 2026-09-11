package com.tangem.tests.tangempay

import android.Manifest
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.REFERRAL_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.utils.getWcUri
import com.tangem.common.utils.setClipboardText
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.createConnectionViaPasteFromClipboardButton
import com.tangem.scenarios.openAddFundsChooseTokenSheet
import com.tangem.scenarios.openAddTokensPortfolioSelector
import com.tangem.scenarios.openMainScreenWithPayAndTwoAccounts
import com.tangem.scenarios.openMarketsAddToPortfolioSelector
import com.tangem.scenarios.openReferralPortfolioSelector
import com.tangem.scenarios.openTransferChooseTokenSheet
import com.tangem.scenarios.openWalletConnectScreen
import com.tangem.scenarios.openWalletSettingsScreen
import com.tangem.scenarios.openWcConnectionAccountSelector
import com.tangem.scenarios.setTangemPayWithTwoAccountsScenarios
import com.tangem.screens.onAddToPortfolioScreen
import com.tangem.screens.onChooseTokenBottomSheet
import com.tangem.screens.onPortfolioSelectorSheet
import com.tangem.screens.onWalletSettingsScreen
import com.tangem.wallet.BuildConfig
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayAccountVisibilityTest : BaseTestCase() {

    private val firstAccountName = "Account 1"
    private val secondAccountName = "Account 2"
    private val payAccountName = getResourceString(CoreResR.string.tangempay_payment_account)

    @AllureId("11470")
    @DisplayName("Tangem Pay: the Pay account is not listed among the wallet settings accounts")
    @Test
    fun payAccountNotListedInWalletSettingsTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Pay wallet with two accounts") {
                setTangemPayWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Pay wallet holding two accounts") {
                openMainScreenWithPayAndTwoAccounts()
            }
            step("Open 'Wallet settings' screen") {
                openWalletSettingsScreen()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onWalletSettingsScreen { accountItem(firstAccountName).assertIsDisplayed() }
                }
                onWalletSettingsScreen { accountItem(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$payAccountName' is not displayed") {
                onWalletSettingsScreen { accountItem(payAccountName).assertDoesNotExist() }
            }
        }
    }

    @AllureId("11475")
    @DisplayName("Tangem Pay: the Pay account is not listed in the Add funds token selector")
    @Test
    fun payAccountNotListedInAddFundsSelectorTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Pay wallet with two accounts") {
                setTangemPayWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Pay wallet holding two accounts") {
                openMainScreenWithPayAndTwoAccounts()
            }
            step("Open the 'Choose token' sheet from 'Add funds'") {
                openAddFundsChooseTokenSheet()
            }
            // Only the first account is asserted: the selector lists an account solely when it holds a
            // token eligible for the flow.
            step("Assert '$firstAccountName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onChooseTokenBottomSheet { accountHeader(firstAccountName).assertIsDisplayed() }
                }
            }
            step("Assert '$payAccountName' is not displayed") {
                onChooseTokenBottomSheet { accountHeader(payAccountName).assertDoesNotExist() }
            }
        }
    }

    @AllureId("11476")
    @DisplayName("Tangem Pay: the Pay account is not listed in the Transfers token selector")
    @Test
    fun payAccountNotListedInTransfersSelectorTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Pay wallet with two accounts") {
                setTangemPayWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Pay wallet holding two accounts") {
                openMainScreenWithPayAndTwoAccounts()
            }
            step("Open the 'Choose token' sheet from 'Transfer'") {
                openTransferChooseTokenSheet()
            }
            step("Assert '$firstAccountName' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onChooseTokenBottomSheet { accountHeader(firstAccountName).assertIsDisplayed() }
                }
            }
            step("Assert '$payAccountName' is not displayed") {
                onChooseTokenBottomSheet { accountHeader(payAccountName).assertDoesNotExist() }
            }
        }
    }

    @AllureId("11469")
    @DisplayName("Tangem Pay: the Pay account is not offered when adding a token via Add and Manage")
    @Test
    fun payAccountNotOfferedWhenAddingTokenViaAddAndManageTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Pay wallet with two accounts") {
                setTangemPayWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Pay wallet holding two accounts") {
                openMainScreenWithPayAndTwoAccounts()
            }
            step("Open the account picker behind 'Add tokens'") {
                openAddTokensPortfolioSelector()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onPortfolioSelectorSheet { portfolioOption(firstAccountName).assertIsDisplayed() }
                }
                onPortfolioSelectorSheet { portfolioOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$payAccountName' is not displayed") {
                onPortfolioSelectorSheet { portfolioOption(payAccountName).assertDoesNotExist() }
            }
        }
    }

    @AllureId("11468")
    @DisplayName("Tangem Pay: the Pay account is not offered when adding a token via Markets")
    @Test
    fun payAccountNotOfferedWhenAddingTokenViaMarketsTest() {
        // Held by neither account, so Markets offers to add it.
        val marketsToken = "Bitcoin"

        setupHooks().run {
            step("Set WireMock scenarios for a Pay wallet with two accounts") {
                setTangemPayWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Pay wallet holding two accounts") {
                openMainScreenWithPayAndTwoAccounts()
            }
            step("Open the portfolio picker for '$marketsToken' in Markets") {
                openMarketsAddToPortfolioSelector(marketsToken)
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onAddToPortfolioScreen { walletOption(firstAccountName).assertIsDisplayed() }
                }
                onAddToPortfolioScreen { walletOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$payAccountName' is not displayed") {
                onAddToPortfolioScreen { walletOption(payAccountName).assertDoesNotExist() }
            }
        }
    }

    @AllureId("11471")
    @DisplayName("Tangem Pay: the Pay account is not offered as the referral rewards account")
    @Test
    fun payAccountNotOfferedAsReferralRewardsAccountTest() {
        // The rewards-account row lives on the pre-participation screen.
        val referralNotParticipatingState = "Started"

        setupHooks().run {
            step("Set WireMock scenarios for a Pay wallet with two accounts") {
                setTangemPayWithTwoAccountsScenarios()
            }
            step("Set WireMock scenario '$REFERRAL_API_SCENARIO' to '$referralNotParticipatingState'") {
                setWireMockScenarioState(REFERRAL_API_SCENARIO, referralNotParticipatingState)
            }
            step("Open 'Main' screen with the Pay wallet holding two accounts") {
                openMainScreenWithPayAndTwoAccounts()
            }
            step("Open the referral 'Account for rewards' picker") {
                openReferralPortfolioSelector()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onPortfolioSelectorSheet { portfolioOption(firstAccountName).assertIsDisplayed() }
                }
                onPortfolioSelectorSheet { portfolioOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$payAccountName' is not displayed") {
                onPortfolioSelectorSheet { portfolioOption(payAccountName).assertDoesNotExist() }
            }
        }
    }

    @AllureId("11472")
    @DisplayName("Tangem Pay: the Pay account is not offered when connecting a dApp via WalletConnect")
    @Test
    fun payAccountNotOfferedWhenConnectingDAppViaWalletConnectTest() {
        val context = device.context
        val packageName = BuildConfig.APPLICATION_ID
        val permissionName = Manifest.permission.CAMERA

        setupHooks(
            additionalBeforeSection = {
                device.uiDevice.executeShellCommand("pm grant $packageName $permissionName")
            },
        ).run {
            step("Set WireMock scenarios for a Pay wallet with two accounts") {
                setTangemPayWithTwoAccountsScenarios()
            }
            step("Open 'Main' screen with the Pay wallet holding two accounts") {
                openMainScreenWithPayAndTwoAccounts()
            }
            step("Open 'Wallet Connect' screen") {
                openWalletConnectScreen()
            }
            // Fetched after the import, not before it: a pairing URI expires, and importing the
            // wallet takes minutes.
            step("Set WalletConnect URI to clipboard") {
                setClipboardText(context, getWcUri())
            }
            step("Create connection via 'Paste from clipboard' button") {
                createConnectionViaPasteFromClipboardButton()
            }
            step("Open the account selector on the connection sheet") {
                openWcConnectionAccountSelector()
            }
            step("Assert '$firstAccountName' and '$secondAccountName' are displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onPortfolioSelectorSheet { portfolioOption(firstAccountName).assertIsDisplayed() }
                }
                onPortfolioSelectorSheet { portfolioOption(secondAccountName).assertIsDisplayed() }
            }
            step("Assert '$payAccountName' is not displayed") {
                onPortfolioSelectorSheet { portfolioOption(payAccountName).assertDoesNotExist() }
            }
        }
    }
}