package com.tangem.tests.moneyaccount

import androidx.test.espresso.Espresso
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.PROVIDERS_API_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.extractText
import com.tangem.common.utils.assertClipboardTextEquals
import com.tangem.common.utils.assertTextSharedViaChooser
import com.tangem.common.utils.clearClipboard
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubShareChooserIntent
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.moneyaccount.onMoneyAccountAddFundsSheet
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import com.tangem.screens.tangempay.onTangemPayVaBankingDetailsErrorSheet
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class MoneyAccountTopUpTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val eligibilityState = "PaeraCustomer"
    private val moneyAccountScenario = "tangem_pay_money_account"
    private val moneyAccountActiveState = "AccountActive"

    private val bankCredentialsScenario = "tangem_pay_money_account_bank_credentials"
    private val bankCredentialsSuccessState = "Started"
    private val bankCredentialsNoDepositLimitState = "NoDepositLimit"
    private val bankCredentialsEmptyBankAddressState = "EmptyBankAddress"
    private val bankCredentialsDelayedState = "Delayed"
    private val bankCredentialsErrorState = "Error"
    private val bankCredentialsEndpointPattern = "/bff-v2/v1/account/bank-credentials/mock-money-account-product-id"

    private val usdcBalanceScenario = "money_account_usdc_balance"
    private val usdcBalanceAfterDepositState = "AfterDeposit"
    private val usdcBalanceZeroState = "Zero"
    private val usdcBalanceUnreachableState = "Unreachable"
    private val usdcHistoryScenario = "money_account_usdc_history"
    private val usdcHistoryAfterDepositState = "AfterDeposit"

    private val storiesScenario = "stories_first_time_swap_v2"
    private val storiesErrorState = "Error"

    private val initialBalance = "$25.00"
    private val balanceAfterDeposit = "$35.00"
    private val receivedTitle = getResourceString(CoreResR.string.common_received)

    private val moneyAccountAddress = "0x5aAb90356b4cd7Fb64f7F70dC6fe063A7B52f011"
    private val walletPolygonAddress = "0x3369554B994908d249d307b105F8e5E3115615c2"
    private val usdc = "USDC"

    private val expectedBeneficiaryName = "Money Account User"
    private val expectedBeneficiaryAddress = "2 Market St, San Francisco, CA 94105"
    private val expectedBankName = "SSB Bank"
    private val expectedBankAddress = "8700 Perry Highway, Pittsburgh, PA 15237"
    private val expectedAccountNumber = "707600496086"
    private val expectedRoutingNumber = "043087080"
    private val dailyDepositLimit = "$2,500"
    private val hardcodedDepositLimit = "$10,000"

    private val routeRail = "ACH"
    private val routeFeeValue = "$1"

    @AllureId("13148")
    @DisplayName("Money account: the Add funds sheet offers three top-up methods in order")
    @Test
    fun addFundsSheetShowsThreeTopUpMethodsTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert balance '$initialBalance' is displayed") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT) { balance.assertTextEquals(initialBalance) } }
            }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            step("Assert 'Add funds' title is displayed") {
                onMoneyAccountAddFundsSheet { title.assertIsDisplayed() }
            }
            step("Assert every top-up method is displayed with its description") {
                onMoneyAccountAddFundsSheet {
                    swapOption.assertIsDisplayed()
                    swapOptionDescription.assertIsDisplayed()
                    receiveOption.assertIsDisplayed()
                    receiveOptionDescription.assertIsDisplayed()
                    bankTransferOption.assertIsDisplayed()
                    bankTransferOptionDescription.assertIsDisplayed()
                }
            }
            step("Assert the top-up methods go in order: own wallet, another wallet, bank account") {
                onMoneyAccountAddFundsSheet {
                    val swapTop = topOf(swapOption)
                    val receiveTop = topOf(receiveOption)
                    val bankTransferTop = topOf(bankTransferOption)
                    assertTrue("'From your Tangem Wallet' should go first", swapTop < receiveTop)
                    assertTrue("'From a bank account' should go last", receiveTop < bankTransferTop)
                }
            }
            step("Close 'Add funds' sheet by swiping down") {
                onMoneyAccountAddFundsSheet { dismissBySwipeDown() }
            }
            step("Assert 'Add funds' sheet is not displayed") {
                awaitSuccess { onMoneyAccountAddFundsSheet { bankTransferOption.assertDoesNotExist() } }
            }
            step("Assert 'Money account' screen still shows '$initialBalance'") {
                onMoneyAccountScreen {
                    balance.assertTextEquals(initialBalance)
                    addFundsButton.assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("13149")
    @DisplayName("Money account: with the balance unavailable the Add funds and Send buttons are disabled")
    @Test
    fun actionButtonsDisabledWhenBalanceUnavailableTest() {
        val providersPolygonMockOnlyState = "PolygonMockOnly"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(PROVIDERS_API_SCENARIO, providersPolygonMockOnlyState)
            },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceUnreachableState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceUnreachableState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert 'Balance unavailable' banner is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT) {
                    onMoneyAccountScreen { balanceUnavailableBanner.assertIsDisplayed() }
                }
            }
            step("Assert 'Add funds' button is disabled") {
                onMoneyAccountScreen { addFundsButton.assertIsNotEnabled() }
            }
            step("Assert 'Send' button is disabled") {
                onMoneyAccountScreen { sendButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("13150")
    @DisplayName("Money account: top-up from own Polygon USDC is a transfer that credits the account")
    @Test
    fun topUpFromOwnPolygonUsdcTest() {
        val userTokensUsdcState = "TangemPayTransferUsdc"
        val depositAmount = "10"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
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
                onSwapTokenScreen { flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { addFundsTitle.assertIsDisplayed() } }
            }
            step("Assert '$usdc' is both the source and the recipient") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(usdc).assertIsDisplayed() }
                    receiveTokenSymbol(usdc).assertIsDisplayed()
                }
            }
            step("Enter amount '$depositAmount'") { inputAmount(depositAmount) }
            step("Assert Transfer mode is ready without a provider") { assertTransferReady() }
            step("Assert network fee is displayed") { waitForFeeDisplayed() }
            step("Hold to confirm the transfer") { holdToConfirmTransferUntilInProgress() }
            step("Assert 'Transfer in progress' screen is displayed") {
                onSwapSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { transferInProgressTitle.assertIsDisplayed() }
                }
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Switch WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceAfterDepositState)
            }
            step("Switch WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryAfterDepositState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryAfterDepositState)
            }
            step("Return to 'Main' screen") { returnToMainFromMoneyAccount() }
            step("Refresh 'Main' until the account balance is '$balanceAfterDeposit'") {
                refreshMainUntilMoneyAccountBalance(balanceAfterDeposit)
            }
            step("Click on 'Money account' row") { clickMoneyAccountRowUntilDetailsOpen() }
            step("Assert balance '$balanceAfterDeposit' is displayed") {
                onMoneyAccountScreen { balance.assertTextEquals(balanceAfterDeposit) }
            }
            step("Pull to refresh 'Money account' screen") { refreshMoneyAccountUntilTransaction(receivedTitle) }
            step("Assert incoming '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(receivedTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13151")
    @DisplayName("Money account: top-up swaps the most funded token into the account USDC")
    @Test
    fun topUpByExchangingAnotherTokenTest() {
        val bitcoinScenario = "bitcoin_utxo"
        val bitcoinBalanceState = "BalanceHotWalletSvS"
        val expressAssetsScenario = "express_api_assets"
        val expressAssetsState = "BitcoinExchangeEnabled"
        val bitcoin = "BTC"
        val swapFromAmount = "0.001"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
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
            step("Set WireMock scenario '$expressAssetsScenario' to '$expressAssetsState'") {
                setWireMockScenarioState(expressAssetsScenario, expressAssetsState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            step("Click on 'From your Tangem Wallet' option") {
                onMoneyAccountAddFundsSheet { swapOption.performClick() }
            }
            step("Assert 'Add funds' Swap screen is displayed") {
                onSwapTokenScreen { flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { addFundsTitle.assertIsDisplayed() } }
            }
            step("Assert '$bitcoin' is pre-selected as the source and '$usdc' is the recipient") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(bitcoin).assertIsDisplayed() }
                    receiveTokenSymbol(usdc).assertIsDisplayed()
                }
            }
            step("Enter swap amount '$swapFromAmount'") {
                onSwapTokenScreen {
                    textInput.performClick()
                    textInput.performTextReplacement(swapFromAmount)
                }
            }
            step("Close the keyboard") {
                Espresso.closeSoftKeyboard()
                waitForIdle()
            }
            step("Assert provider quote and network fee are displayed") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        providersBlock.assertIsDisplayed()
                        feeAmount.assertIsDisplayed()
                    }
                }
            }
            step("Confirm swap by holding the button") { confirmSwapByHolding() }
            step("Assert 'Swap in progress' screen is displayed") {
                onSwapSuccessScreen { flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { title.assertIsDisplayed() } }
            }
            step("Click on 'Close' button") { onSwapSuccessScreen { closeButton.performClick() } }
            step("Switch WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceAfterDepositState)
            }
            step("Switch WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryAfterDepositState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryAfterDepositState)
            }
            step("Return to 'Main' screen") { returnToMainFromMoneyAccount() }
            step("Refresh 'Main' until the account balance is '$balanceAfterDeposit'") {
                refreshMainUntilMoneyAccountBalance(balanceAfterDeposit)
            }
            step("Click on 'Money account' row") { clickMoneyAccountRowUntilDetailsOpen() }
            step("Assert balance '$balanceAfterDeposit' is displayed") {
                onMoneyAccountScreen { balance.assertTextEquals(balanceAfterDeposit) }
            }
            step("Pull to refresh 'Money account' screen") { refreshMoneyAccountUntilTransaction(receivedTitle) }
            step("Assert incoming '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(receivedTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13152")
    @DisplayName("Money account: the top-up recipient is locked to the account USDC")
    @Test
    fun topUpRecipientCannotBeChangedTest() {
        val userTokensUsdcState = "TangemPayTransferUsdc"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
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
            step("Assert the recipient is '$usdc' without a token selector") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveTokenSymbol(usdc).assertIsDisplayed() }
                    receiveSelectTokenIcon.assertDoesNotExist()
                }
            }
            step("Click on the recipient token") {
                onSwapTokenScreen { receiveTokenSymbol(usdc).performClick() }
            }
            step("Assert 'Add funds' Swap screen stays open with '$usdc' as the recipient") {
                onSwapTokenScreen {
                    addFundsTitle.assertIsDisplayed()
                    receiveTokenSymbol(usdc).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("13153")
    @DisplayName("Money account: top-up from an empty wallet reports insufficient funds")
    @Test
    fun topUpWithoutFundsShowsInsufficientFundsTest() {
        val userTokensPolygonOnlyState = "PolygonOnly"
        val polygonBalanceScenario = "polygon_coin_balance"
        val polygonZeroBalanceState = "ZeroBalance"
        val polygonPairsScenario = "polygon_pos_pairs"
        val polygonToUsdcPairState = "PolUsdc"
        val polygon = "POL"
        val amount = "1"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(storiesScenario, storiesErrorState) },
        ).run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$USER_TOKENS_API_SCENARIO' to '$userTokensPolygonOnlyState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensPolygonOnlyState)
            }
            step("Set WireMock scenario '$polygonBalanceScenario' to '$polygonZeroBalanceState'") {
                setWireMockScenarioState(polygonBalanceScenario, polygonZeroBalanceState)
            }
            step("Set WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceZeroState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceZeroState)
            }
            step("Set WireMock scenario '$polygonPairsScenario' to '$polygonToUsdcPairState'") {
                setWireMockScenarioState(polygonPairsScenario, polygonToUsdcPairState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            step("Click on 'From your Tangem Wallet' option") {
                onMoneyAccountAddFundsSheet { swapOption.performClick() }
            }
            step("Assert '$polygon' is the source and '$usdc' is the recipient") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { swapTokenSymbol(polygon).assertIsDisplayed() }
                    receiveTokenSymbol(usdc).assertIsDisplayed()
                }
            }
            step("Enter amount '$amount'") { inputAmount(amount) }
            step("Assert 'Insufficient funds' is displayed and the main button is disabled") {
                onSwapTokenScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { insufficientFundsButton.assertIsDisplayed() }
                    insufficientFundsButton.assertIsNotEnabled()
                }
            }
        }
    }

    @AllureId("13154")
    @DisplayName("Money account: Receive shows the account address, not the wallet Polygon address")
    @Test
    fun receiveShowsAccountAddressTest() {
        val polygonNetwork = "Polygon"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Stub the system share chooser") { stubShareChooserIntent() }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            step("Click on 'From another wallet or exchange' option") {
                onMoneyAccountAddFundsSheet { receiveOption.performClick() }
            }
            step("Assert the '$polygonNetwork' network warning is displayed") {
                onTokenReceiveWarningBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { bottomSheet.assertIsDisplayed() }
                    networkName(polygonNetwork).assertIsDisplayed()
                }
            }
            step("Click on 'Got it' button") { onTokenReceiveWarningBottomSheet { gotItButton.performClick() } }
            step("Click on 'Show QR code' button") {
                onReceiveAssetsBottomSheet { showQrCodeButton().clickWithAssertion() }
            }
            var displayedAddress = ""
            step("Assert the '$usdc' account address is displayed instead of the wallet Polygon address") {
                onTokenReceiveQrCodeBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { address.assertIsDisplayed() }
                    title.assertTextContains(usdc, substring = true)
                    displayedAddress = address.extractText()
                }
                assertEquals(moneyAccountAddress.lowercase(), displayedAddress.lowercase())
                assertNotEquals(walletPolygonAddress.lowercase(), displayedAddress.lowercase())
            }
            step("Clear the clipboard") { clearClipboard() }
            step("Click on 'Copy' button") { onTokenReceiveQrCodeBottomSheet { copyButton.performClick() } }
            step("Assert the account address is copied to the clipboard") {
                assertClipboardTextEquals(displayedAddress)
            }
            step("Click on 'Share' button") { onTokenReceiveQrCodeBottomSheet { shareButton.performClick() } }
            step("Assert the account address is passed to the system share chooser") {
                assertTextSharedViaChooser(displayedAddress, times = 1)
            }
        }
    }

    @AllureId("13155")
    @DisplayName("Money account: an external USDC transfer to the account address is credited")
    @Test
    fun externalTransferToAccountAddressIsCreditedTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Assert balance '$initialBalance' is displayed") {
                onMoneyAccountScreen { flakySafely(WAIT_UNTIL_TIMEOUT) { balance.assertTextEquals(initialBalance) } }
            }
            step("Switch WireMock scenario '$usdcBalanceScenario' to '$usdcBalanceAfterDepositState'") {
                setWireMockScenarioState(usdcBalanceScenario, usdcBalanceAfterDepositState)
            }
            step("Switch WireMock scenario '$usdcHistoryScenario' to '$usdcHistoryAfterDepositState'") {
                setWireMockScenarioState(usdcHistoryScenario, usdcHistoryAfterDepositState)
            }
            step("Return to 'Main' screen") { returnToMainFromMoneyAccount() }
            step("Refresh 'Main' until the account balance is '$balanceAfterDeposit'") {
                refreshMainUntilMoneyAccountBalance(balanceAfterDeposit)
            }
            step("Click on 'Money account' row") { clickMoneyAccountRowUntilDetailsOpen() }
            step("Assert balance '$balanceAfterDeposit' is displayed") {
                onMoneyAccountScreen { balance.assertTextEquals(balanceAfterDeposit) }
            }
            step("Pull to refresh 'Money account' screen") { refreshMoneyAccountUntilTransaction(receivedTitle) }
            step("Assert incoming '$receivedTitle' transaction is displayed") {
                onTxHistoryScreen { transactionItem(receivedTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("13156")
    @DisplayName("Money account: the bank transfer opens the account details with fees, copy and share")
    @Test
    fun bankTransferShowsAccountDetailsTest() {
        val expectedSharedText = listOf(
            VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER to expectedAccountNumber,
            VirtualAccountRequisitesBottomSheetPageObject.ROUTING_NUMBER to expectedRoutingNumber,
            VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_NAME to expectedBeneficiaryName,
            VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_ADDRESS to expectedBeneficiaryAddress,
            VirtualAccountRequisitesBottomSheetPageObject.BANK_NAME to expectedBankName,
            VirtualAccountRequisitesBottomSheetPageObject.BANK_ADDRESS to expectedBankAddress,
        ).joinToString(separator = "\n") { (field, value) -> "$field: $value" }

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$bankCredentialsScenario' to '$bankCredentialsDelayedState'") {
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsDelayedState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Stub the system share chooser") { stubShareChooserIntent() }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            step("Click on 'From a bank account' option") {
                onMoneyAccountAddFundsSheet { bankTransferOption.performClick() }
            }
            step("Assert 'From a bank account' option shows a loader") {
                onMoneyAccountAddFundsSheet { bankTransferLoader.assertIsDisplayed() }
            }
            step("Assert 'Account details' screen is displayed") {
                onVirtualAccountRequisitesBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { accountDetailsTitle.assertIsDisplayed() }
                }
            }
            step("Assert every banking detail matches the bank-credentials response") {
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER,
                    expectedValue = expectedAccountNumber,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ROUTING_NUMBER,
                    expectedValue = expectedRoutingNumber,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_NAME,
                    expectedValue = expectedBeneficiaryName,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_ADDRESS,
                    expectedValue = expectedBeneficiaryAddress,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BANK_NAME,
                    expectedValue = expectedBankName,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BANK_ADDRESS,
                    expectedValue = expectedBankAddress,
                )
            }
            step("Copy the account number and assert it reaches the clipboard") {
                copyVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER,
                    expectedValue = expectedAccountNumber,
                )
            }
            step("Assert the '$routeRail' route fee '$routeFeeValue' and the SWIFT note are displayed") {
                onVirtualAccountRequisitesBottomSheet {
                    feeTitle(routeRail).performScrollTo()
                    feeTitle(routeRail).assertTextEquals(routeRail)
                    feeValue(routeRail).assertTextEquals(routeFeeValue)
                    swiftWarning.performScrollTo()
                    swiftWarning.assertIsDisplayed()
                }
            }
            step("Click on 'Share' button") {
                onVirtualAccountRequisitesBottomSheet {
                    shareButton.performClick()
                }
            }
            step("Assert every banking detail is passed to the system share chooser") {
                assertTextSharedViaChooser(expectedSharedText, times = 1)
            }
        }
    }

    @AllureId("13157")
    @DisplayName("Money account: the daily deposit limit comes from the bank-credentials response")
    @Test
    fun bankTransferDepositLimitFromBackendTest() {
        val expectedDepositLimitHint = getResourceString(
            CoreResR.string.tangempay_bank_transfer_details_daily_deposits_hint,
            dailyDepositLimit,
        )

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$bankCredentialsScenario' to '$bankCredentialsSuccessState'") {
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsSuccessState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Account details' screen") { openMoneyAccountBankDetails() }
            step("Assert the daily deposit limit is '$dailyDepositLimit', not '$hardcodedDepositLimit'") {
                onVirtualAccountRequisitesBottomSheet {
                    depositLimitSubtitle.performScrollTo()
                    depositLimitSubtitle.assertTextEquals(expectedDepositLimitHint)
                }
            }
            step("Click on system 'Back' button") { device.uiDevice.pressBack() }
            step("Assert 'Money account' screen is displayed") {
                awaitSuccess { onMoneyAccountScreen { addFundsButton.assertIsDisplayed() } }
            }
            step("Switch WireMock scenario '$bankCredentialsScenario' to '$bankCredentialsNoDepositLimitState'") {
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsNoDepositLimitState)
            }
            step("Open 'Account details' screen again") { openMoneyAccountBankDetails() }
            step("Assert the daily deposit limit block is not displayed") {
                onVirtualAccountRequisitesBottomSheet {
                    accountNumber.assertTextEquals(expectedAccountNumber)
                    depositLimitTitle.assertDoesNotExist()
                    depositLimitSubtitle.assertDoesNotExist()
                }
            }
        }
    }

    @AllureId("13158")
    @DisplayName("Money account: a failed bank-credentials request shows the error sheet, Retry loads the details")
    @Test
    fun bankingDetailsErrorRetryLoadsDetailsTest() {
        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$bankCredentialsScenario' to '$bankCredentialsErrorState'") {
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsErrorState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            step("Click on 'From a bank account' option") {
                onMoneyAccountAddFundsSheet { bankTransferOption.performClick() }
            }
            step("Assert the banking details error sheet is displayed") {
                onTangemPayVaBankingDetailsErrorSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { title.assertIsDisplayed() }
                    description.assertIsDisplayed()
                    contactSupportButton.assertIsDisplayed()
                    retryButton.assertIsDisplayed()
                }
            }
            step("Assert 'Account details' screen is not displayed") {
                onVirtualAccountRequisitesBottomSheet { accountDetailsTitle.assertDoesNotExist() }
            }
            step("Stub the system share chooser") { stubShareChooserIntent() }
            step("Click on 'Contact support' button") {
                onTangemPayVaBankingDetailsErrorSheet { contactSupportButton.performClick() }
            }
            step("Assert the error sheet is closed and 'Money account' screen is displayed") {
                awaitSuccess { onTangemPayVaBankingDetailsErrorSheet { title.assertDoesNotExist() } }
                onMoneyAccountScreen { addFundsButton.assertIsDisplayed() }
            }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            step("Click on 'From a bank account' option") {
                onMoneyAccountAddFundsSheet { bankTransferOption.performClick() }
            }
            step("Assert the banking details error sheet is displayed") {
                onTangemPayVaBankingDetailsErrorSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { retryButton.assertIsDisplayed() }
                }
            }
            step("Switch WireMock scenario '$bankCredentialsScenario' to '$bankCredentialsSuccessState'") {
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsSuccessState)
            }
            val credentialsRequestsBefore = getWireMockRequestCount("GET", bankCredentialsEndpointPattern)
            step("Click on 'Retry' button") {
                onTangemPayVaBankingDetailsErrorSheet { retryButton.clickWithAssertion() }
            }
            step("Assert 'Account details' screen shows the banking details") {
                onVirtualAccountRequisitesBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { accountNumber.assertTextEquals(expectedAccountNumber) }
                    routingNumber.assertTextEquals(expectedRoutingNumber)
                }
            }
            step("Assert Retry requested the banking details again") {
                val credentialsRequestsAfter = getWireMockRequestCount("GET", bankCredentialsEndpointPattern)
                assertTrue(
                    "Retry should request $bankCredentialsEndpointPattern again",
                    credentialsRequestsAfter > credentialsRequestsBefore,
                )
            }
        }
    }

    @AllureId("13160")
    @DisplayName("Money account: an empty banking detail is hidden on the screen and in Share")
    @Test
    fun bankTransferEmptyDetailIsHiddenTest() {
        val expectedSharedText = listOf(
            VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER to expectedAccountNumber,
            VirtualAccountRequisitesBottomSheetPageObject.ROUTING_NUMBER to expectedRoutingNumber,
            VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_NAME to expectedBeneficiaryName,
            VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_ADDRESS to expectedBeneficiaryAddress,
            VirtualAccountRequisitesBottomSheetPageObject.BANK_NAME to expectedBankName,
        ).joinToString(separator = "\n") { (field, value) -> "$field: $value" }

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$bankCredentialsScenario' to '$bankCredentialsEmptyBankAddressState'") {
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsEmptyBankAddressState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Stub the system share chooser") { stubShareChooserIntent() }
            step("Open 'Account details' screen") { openMoneyAccountBankDetails() }
            step("Assert the filled banking details are displayed") {
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER,
                    expectedValue = expectedAccountNumber,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ROUTING_NUMBER,
                    expectedValue = expectedRoutingNumber,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_NAME,
                    expectedValue = expectedBeneficiaryName,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_ADDRESS,
                    expectedValue = expectedBeneficiaryAddress,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BANK_NAME,
                    expectedValue = expectedBankName,
                )
            }
            step("Assert the empty 'Bank address' row is not displayed") {
                onVirtualAccountRequisitesBottomSheet {
                    requisiteValue(VirtualAccountRequisitesBottomSheetPageObject.BANK_ADDRESS).assertDoesNotExist()
                }
            }
            step("Click on 'Share' button") {
                onVirtualAccountRequisitesBottomSheet {
                    shareButton.performClick()
                }
            }
            step("Assert the shared text leaves the empty 'Bank address' out") {
                assertTextSharedViaChooser(expectedSharedText, times = 1)
            }
        }
    }

    @AllureId("13161")
    @DisplayName("Money account: repeated taps on the loading bank transfer send one request and open one screen")
    @Test
    fun bankTransferRepeatedTapIsIgnoredTest() {
        val repeatedTaps = 2

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$moneyAccountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, moneyAccountActiveState)
            }
            step("Set WireMock scenario '$bankCredentialsScenario' to '$bankCredentialsDelayedState'") {
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsDelayedState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Add funds' sheet") { openMoneyAccountAddFunds() }
            val credentialsRequestsBefore = getWireMockRequestCount("GET", bankCredentialsEndpointPattern)
            step("Click on 'From a bank account' option") {
                onMoneyAccountAddFundsSheet { bankTransferOption.performClick() }
            }
            step("Assert 'From a bank account' option shows a loader") {
                onMoneyAccountAddFundsSheet { bankTransferLoader.assertIsDisplayed() }
            }
            repeat(repeatedTaps) {
                step("Click on 'From a bank account' option while it is loading") {
                    onMoneyAccountAddFundsSheet { bankTransferOption.performClick() }
                }
            }
            step("Assert 'Account details' screen is displayed") {
                onVirtualAccountRequisitesBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { accountNumber.assertTextEquals(expectedAccountNumber) }
                }
            }
            step("Assert the banking details were requested once") {
                val credentialsRequestsAfter = getWireMockRequestCount("GET", bankCredentialsEndpointPattern)
                assertEquals(1, credentialsRequestsAfter - credentialsRequestsBefore)
            }
            step("Click on system 'Back' button") { device.uiDevice.pressBack() }
            step("Assert one 'Back' returns to 'Money account' screen") {
                awaitSuccess { onMoneyAccountScreen { addFundsButton.assertIsDisplayed() } }
                onVirtualAccountRequisitesBottomSheet { accountDetailsTitle.assertDoesNotExist() }
            }
        }
    }
}