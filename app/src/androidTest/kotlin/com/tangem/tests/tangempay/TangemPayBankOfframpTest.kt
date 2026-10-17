package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankAccounts.ACME
import com.tangem.common.constants.TangemPayBankAccounts.ALICE
import com.tangem.common.constants.TangemPayBankAccounts.BOB
import com.tangem.common.constants.TangemPayBankAccounts.CAROL
import com.tangem.common.constants.TangemPayBankAccounts.JANE
import com.tangem.common.constants.TangemPayBankAccounts.JOHN
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ACCESS_CODE
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.onSwapSuccessScreen
import com.tangem.screens.onSwapTokenScreen
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayBankOfframpTest : BaseTestCase() {

    private val bankAccountsScenario = "tangem_pay_bank_accounts"
    private val bankOfframpOptionsScenario = "tangem_pay_bank_offramp_options"
    private val bankAccountsEndpoint = "/bff-v2/v1/account/bank-accounts"
    private val bankOfframpOptionsEndpoint = "/bff-v2/v1/account/bank-offramp/options"
    private val eligibilityChannelsEndpoint = "/bff-v2/v1/eligibility/channels"

    @AllureId("12172")
    @DisplayName("Tangem Pay: Withdraw opens the hub only when the channels include BANK_OFFRAMP")
    @Test
    fun withdrawOpensHubOnlyWithBankOfframpChannelTest() {
        val channelsWithoutBankOfframpState = "BannerAndDetails"
        val swapStoriesScenario = "stories_first_time_swap_v2"
        val swapStoriesErrorState = "Error"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(swapStoriesScenario, swapStoriesErrorState) },
        ).run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Return to Payment account") { returnToPaymentAccountFromWithdrawHub() }
            step("Return to the Main screen") { returnToMainFromTangemPay() }
            step(
                "Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to " +
                    "'$channelsWithoutBankOfframpState'",
            ) {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsWithoutBankOfframpState)
            }
            val channelsRequestsBefore = getWireMockRequestCount("GET", eligibilityChannelsEndpoint)
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.performClick() }
            }
            step("Assert eligibility channels are requested again") {
                assertTangemPayRequestSent("GET", eligibilityChannelsEndpoint, channelsRequestsBefore)
            }
            step("Click on 'Withdraw' action chip") {
                onTangemPayMainScreen { withdrawButton.clickWithAssertion() }
            }
            step("Assert 'Withdraw' Swap screen is displayed") {
                flakySafely { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }
            }
            step("Assert 'Withdraw' hub screen is not displayed") {
                onTangemPayWithdrawHubScreen { screen.assertDoesNotExist() }
            }
        }
    }

    @AllureId("12173")
    @DisplayName("Tangem Pay: the Withdraw hub shows the recents zero state when nothing was withdrawn yet")
    @Test
    fun withdrawHubShowsRecentsZeroStateTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Assert 'Transfer or swap' recents zero state is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { swapRecentsEmpty.assertIsDisplayed() } }
            }
            step("Assert 'Bank transfer' recents zero state is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { bankRecentsEmpty.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12174")
    @DisplayName("Tangem Pay: a failed bank accounts load shows the reload chip that requests the list again")
    @Test
    fun bankAccountsLoadErrorReloadsListTest() {
        val bankAccountsErrorState = "Error"

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Set WireMock scenario '$bankAccountsScenario' to '$bankAccountsErrorState'") {
                setWireMockScenarioState(bankAccountsScenario, bankAccountsErrorState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Assert 'Couldn't load list. Tap to reload' chip is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { recentsLoadError.assertIsDisplayed() } }
            }
            val bankAccountsRequestsBefore = getWireMockRequestCount("GET", bankAccountsEndpoint)
            step("Click on 'Couldn't load list. Tap to reload' chip") {
                onTangemPayWithdrawHubScreen { recentsLoadError.performClick() }
            }
            step("Assert bank accounts are requested again") {
                assertTangemPayRequestSent("GET", bankAccountsEndpoint, bankAccountsRequestsBefore)
            }
        }
    }

    @AllureId("12175")
    @DisplayName("Tangem Pay: the Bank transfer block is unavailable when the off-ramp options fail or are empty")
    @Test
    fun bankTransferUnavailableWithoutOfframpOptionsTest() {
        val optionsErrorState = "Error"
        val optionsEmptyState = "Empty"

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Set WireMock scenario '$bankOfframpOptionsScenario' to '$optionsErrorState'") {
                setWireMockScenarioState(bankOfframpOptionsScenario, optionsErrorState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Assert the Bank transfer block is unavailable") { assertTangemPayBankTransferBlockUnavailable() }
            step("Return to Payment account") { returnToPaymentAccountFromWithdrawHub() }
            step("Set WireMock scenario '$bankOfframpOptionsScenario' to '$optionsEmptyState'") {
                setWireMockScenarioState(bankOfframpOptionsScenario, optionsEmptyState)
            }
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Assert the Bank transfer block is unavailable") { assertTangemPayBankTransferBlockUnavailable() }
        }
    }

    @AllureId("12177")
    @DisplayName("Tangem Pay: the Bank transfer block shows up to three recent recipients and 'See all'")
    @Test
    fun recentBankTransfersListTest() {
        val twoAccountsState = "TwoAccounts"
        val fiveAccountsState = "FiveAccounts"
        val businessLabel = getResourceString(CoreResR.string.tangempay_withdraw_bank_recipient_business)

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Assert recent recipient '${JOHN.shortName}' is displayed") {
                assertTangemPayRecentRecipientDisplayed(JOHN)
            }
            step("Assert recent recipient '${ACME.shortName}' is displayed") {
                assertTangemPayRecentRecipientDisplayed(ACME)
            }
            step("Scroll the Bank transfer block to the end") {
                onTangemPayWithdrawHubScreen { scrollBankListToEnd() }
            }
            step("Assert there is no third recent recipient") {
                onTangemPayWithdrawHubScreen { recentRecipientAt(index = 2).assertDoesNotExist() }
            }
            step("Assert 'See all' chip is not displayed") {
                onTangemPayWithdrawHubScreen { seeAllChip.assertDoesNotExist() }
            }
            step("Return to Payment account") { returnToPaymentAccountFromWithdrawHub() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$fiveAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, fiveAccountsState)
            }
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            listOf(JOHN, ACME, ALICE).forEach { recipient ->
                step("Assert recent recipient '${recipient.shortName}' is displayed") {
                    assertTangemPayRecentRecipientDisplayed(recipient)
                }
            }
            step("Assert 'See all' chip is displayed") {
                onTangemPayWithdrawHubScreen {
                    scrollToSeeAllChip()
                    seeAllChip.assertIsDisplayed()
                }
            }
            step("Assert the fourth recipient '${BOB.shortName}' is not displayed") {
                onTangemPayWithdrawHubScreen { recentRecipientWithName(BOB.shortName).assertDoesNotExist() }
            }
            step("Open 'Bank transfers' list") { openBankRecipientsFromWithdrawHub() }
            step("Assert all five bank accounts are displayed") {
                onTangemPayBankRecipientsScreen {
                    listOf(JOHN, ALICE, BOB, CAROL).forEach { recipient ->
                        flakySafely {
                            recipientRow(recipient.initial, recipient.fullName, recipient.accountNumber)
                                .assertIsDisplayed()
                        }
                    }
                    recipientRow(ACME.initial, ACME.fullName, "${ACME.accountNumber} · $businessLabel")
                        .assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("12178")
    @DisplayName("Tangem Pay: a bank recipient is added to the Withdraw hub recents only after a successful transfer")
    @Test
    fun recipientAddedToRecentsAfterSuccessfulTransferTest() {
        val bankAccountsAfterTransferState = "AfterTransfer"
        val bankAmount = "5"
        val sendingTitle = getResourceString(CoreResR.string.tangempay_withdraw_bank_success_title, "$$bankAmount")
        val transferTypeTitle = getResourceString(CoreResR.string.tangempay_withdraw_bank_transfer_type_title)
        val thirdPartyAccount = getResourceString(CoreResR.string.tangempay_withdraw_bank_transfer_type_third_party)
        val accountTypeTitle = getResourceString(CoreResR.string.tangempay_withdraw_bank_account_type_title)
        val checkingAccount = getResourceString(CoreResR.string.tangempay_withdraw_bank_account_type_checking)

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Assert 'Bank transfer' recents zero state is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { bankRecentsEmpty.assertIsDisplayed() } }
            }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Fill in transfer amount '$bankAmount'") { enterTangemPayBankWithdrawAmount(bankAmount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Choose '$thirdPartyAccount' transfer type") {
                chooseTangemPayBankWithdrawOption(transferTypeTitle, thirdPartyAccount)
            }
            step("Choose '$checkingAccount' account type") {
                chooseTangemPayBankWithdrawOption(accountTypeTitle, checkingAccount)
            }
            step("Fill in recipient details of '${JANE.fullName}'") { fillTangemPayBankRecipientDetails(JANE) }
            step("Fill in bank details of '${JANE.bankName}'") { fillTangemPayBankDetails(JANE) }
            step("Assert 'Confirm transfer' screen is displayed") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { title.assertIsDisplayed() } }
            }
            step("Send the bank transfer") { sendTangemPayBankWithdrawal(TANGEM_PAY_ACCESS_CODE) }
            step("Assert '$sendingTitle' screen is displayed") {
                flakySafely { onTangemPayBankWithdrawSuccessScreen { title(sendingTitle).assertIsDisplayed() } }
            }
            step("Close 'Sending' screen") { closeTangemPayBankWithdrawSuccess() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$bankAccountsAfterTransferState'") {
                setWireMockScenarioState(bankAccountsScenario, bankAccountsAfterTransferState)
            }
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Assert recent recipient '${JANE.shortName}' is displayed") {
                assertTangemPayRecentRecipientDisplayed(JANE)
            }
        }
    }

    @AllureId("12670")
    @DisplayName("Tangem Pay: a token is added to the Withdraw hub 'Transfer or swap' recents only after a successful swap")
    @Test
    fun tokenAddedToRecentsAfterSuccessfulSwapTest() {
        val channelsBankOfframpState = "BankOfframp"
        val swapStoriesScenario = "stories_first_time_swap_v2"
        val swapStoriesErrorState = "Error"
        val quotesScenario = "quotes_api"
        val quotesWithUsdtAndUsdcState = "USDTAndUSDC"
        val swapSourceToken = "USDT"
        val swapSourceNetwork = "BNB Smart Chain"
        val swapReceiveToken = "Bitcoin"
        val swapAmount = "5"

        setupHooks(
            additionalBeforeAppLaunchSection = { setWireMockScenarioState(swapStoriesScenario, swapStoriesErrorState) },
        ).run {
            step("Set WireMock scenarios for a multichain Tangem Pay withdrawal") {
                setTangemPayMultiNetworkWithdrawScenarios()
            }
            step("Set WireMock scenario '$quotesScenario' to '$quotesWithUsdtAndUsdcState'") {
                setWireMockScenarioState(quotesScenario, quotesWithUsdtAndUsdcState)
            }
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' to '$channelsBankOfframpState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsBankOfframpState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Assert 'Transfer or swap' recents zero state is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { swapRecentsEmpty.assertIsDisplayed() } }
            }
            step("Click on 'Within your portfolio' card") {
                onTangemPayWithdrawHubScreen { withinPortfolioCard.performClick() }
            }
            step("Assert 'Withdraw' Swap screen is displayed") {
                flakySafely { onSwapTokenScreen { withdrawTitle.assertIsDisplayed() } }
            }
            step("Choose source token '$swapSourceToken' on '$swapSourceNetwork'") {
                chooseWithdrawSourceToken(tokenName = swapSourceToken, networkName = swapSourceNetwork)
            }
            step("Choose receive token '$swapReceiveToken'") { chooseWithdrawReceiveToken(swapReceiveToken) }
            step("Enter withdraw amount '$swapAmount'") { enterWithdrawAmount(swapAmount) }
            step("Assert receive amount is displayed") {
                onSwapTokenScreen { flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { receiveAmount.assertIsDisplayed() } }
            }
            step("Confirm swap by holding the button") { confirmTangemPayWithdrawByHolding(TANGEM_PAY_ACCESS_CODE) }
            step("Assert 'Swap in progress' screen is displayed") {
                onSwapSuccessScreen { flakySafely(WAIT_UNTIL_TIMEOUT_VERY_LONG) { title.assertIsDisplayed() } }
            }
            step("Click on 'Close' button") {
                onSwapSuccessScreen { closeButton.performClick() }
            }
            step("Assert 'Withdraw' hub screen is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
            }
            step("Return to Payment account") { returnToPaymentAccountFromWithdrawHub() }
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Assert recent token '$swapReceiveToken' is displayed in 'Transfer or swap'") {
                flakySafely { onTangemPayWithdrawHubScreen { recentToken(swapReceiveToken).assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12180")
    @DisplayName("Tangem Pay: the bank accounts list is searched by name and by account number")
    @Test
    fun bankRecipientsSearchTest() {
        val fiveAccountsState = "FiveAccounts"
        val nameQuery = "Ali"
        val accountNumberQuery = "0000"
        val unknownQuery = "zzqx"

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Set WireMock scenario '$bankAccountsScenario' to '$fiveAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, fiveAccountsState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Open 'Bank transfers' list") { openBankRecipientsFromWithdrawHub() }
            step("Assert all five bank accounts are displayed") {
                onTangemPayBankRecipientsScreen {
                    listOf(JOHN, ACME, ALICE, BOB, CAROL).forEach { recipient ->
                        flakySafely { recipientRowWithName(recipient.fullName).assertIsDisplayed() }
                    }
                }
            }
            step("Click on 'Search' field") {
                onTangemPayBankRecipientsScreen { searchField.performClick() }
            }
            step("Search by name '$nameQuery'") { searchTangemPayBankRecipients(nameQuery) }
            step("Assert only '${ALICE.fullName}' is displayed") {
                onTangemPayBankRecipientsScreen {
                    flakySafely { recipientRowWithName(ALICE.fullName).assertIsDisplayed() }
                    listOf(JOHN, ACME, BOB, CAROL).forEach { recipient ->
                        recipientRowWithName(recipient.fullName).assertDoesNotExist()
                    }
                }
            }
            step("Search by account number '$accountNumberQuery'") {
                searchTangemPayBankRecipients(accountNumberQuery)
            }
            step("Assert only the accounts whose number contains '$accountNumberQuery' are displayed") {
                onTangemPayBankRecipientsScreen {
                    listOf(ALICE, BOB, CAROL).forEach { recipient ->
                        flakySafely { recipientRowWithName(recipient.fullName).assertIsDisplayed() }
                    }
                    listOf(JOHN, ACME).forEach { recipient ->
                        recipientRowWithName(recipient.fullName).assertDoesNotExist()
                    }
                }
            }
            step("Search by '$unknownQuery'") { searchTangemPayBankRecipients(unknownQuery) }
            step("Assert 'Nothing found. Try something else' is displayed") {
                flakySafely { onTangemPayBankRecipientsScreen { noResults.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("12181")
    @DisplayName("Tangem Pay: a recipient picked from the full list opens Amount with the recipient prefilled")
    @Test
    fun recipientFromFullListOpensAmountTest() {
        val fiveAccountsState = "FiveAccounts"
        val amount = "5"

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Set WireMock scenario '$bankAccountsScenario' to '$fiveAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, fiveAccountsState)
            }
            step("Open Tangem Pay 'Withdraw' hub") { openTangemPayWithdrawHub() }
            step("Open 'Bank transfers' list") { openBankRecipientsFromWithdrawHub() }
            step("Click on '${ALICE.fullName}'") {
                onTangemPayBankRecipientsScreen {
                    flakySafely { recipientRowWithName(ALICE.fullName).assertIsDisplayed() }
                    recipientRowWithName(ALICE.fullName).performClick()
                }
            }
            step("Assert 'Amount' screen for '${ALICE.displayName}' is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawAmountScreen { headerTitle(ALICE.displayName).assertIsDisplayed() }
                }
            }
            step("Fill in transfer amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert 'Confirm transfer' screen is displayed") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { title.assertIsDisplayed() } }
            }
            step("Assert the recipient info matches '${ALICE.fullName}'") {
                onTangemPayBankWithdrawPreviewScreen {
                    listOf(
                        ALICE.accountNumber,
                        ALICE.routingNumber,
                        ALICE.firstName,
                        ALICE.lastName,
                        ALICE.bankName,
                    ).forEach { value ->
                        detailValue(value).performScrollTo()
                        detailValue(value).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @AllureId("12182")
    @DisplayName("Tangem Pay: the bank transfer source list shows only the tokens with a positive balance")
    @Test
    fun bankTransferSourceTokensTest() {
        val balanceScenario = "tangem_pay_balance_update"
        val arbitrumTopUpState = "MultiNetworkArbitrumTopUp"
        val usdc = "USDC"
        val usdt = "USDT"
        val polygon = "Polygon"
        val arbitrum = "Arbitrum One"
        val bnbSmartChain = "BNB Smart Chain"
        val polygonBalance = "$10"
        val arbitrumBalance = "$5"
        val toppedUpBalance = "22"
        val offrampUnavailable = getResourceString(CoreResR.string.tangempay_withdraw_bank_offramp_unavailable)

        setupHooks().run {
            step("Set WireMock scenarios for a Tangem Pay customer with the bank off-ramp") {
                setTangemPayBankOfframpScenarios()
            }
            step("Open Tangem Pay") { openTangemPay() }
            val optionsRequestsBefore = getWireMockRequestCount("GET", bankOfframpOptionsEndpoint)
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Assert bank off-ramp options are requested") {
                assertTangemPayRequestSent("GET", bankOfframpOptionsEndpoint, optionsRequestsBefore)
            }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert '$usdc' on '$polygon' is the default source") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { sourceRowWith(usdc, polygon).assertIsDisplayed() } }
            }
            step("Open the source token selector") { openTangemPayBankWithdrawSourceSelector() }
            step("Assert '$usdc' on '$polygon' is listed and selected") {
                onTangemPayBankWithdrawAmountScreen { selectedTokenRow(usdc, "$polygon · $polygonBalance").assertIsDisplayed() }
            }
            step("Assert '$usdt' on '$bnbSmartChain' is listed as '$offrampUnavailable'") {
                onTangemPayBankWithdrawAmountScreen {
                    tokenRow(usdt, "$bnbSmartChain · $offrampUnavailable").assertIsDisplayed()
                }
            }
            step("Assert zero-balance '$usdc' on '$bnbSmartChain' is not listed") {
                onTangemPayBankWithdrawAmountScreen { tokenRow(usdc, bnbSmartChain).assertDoesNotExist() }
            }
            step("Return to Payment account") { returnToPaymentAccountFromBankWithdrawSourceSelector() }
            step("Set WireMock scenario '$balanceScenario' to '$arbitrumTopUpState'") {
                setWireMockScenarioState(balanceScenario, arbitrumTopUpState)
            }
            step("Pull to refresh Tangem Pay") { pullToRefreshTangemPay() }
            step("Assert the topped-up balance is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayMainScreen { balance.assertTextContainsSafe(toppedUpBalance, substring = true) }
                }
            }
            val optionsRequestsBeforeSecondOpen = getWireMockRequestCount("GET", bankOfframpOptionsEndpoint)
            step("Open 'Withdraw' hub") { openWithdrawHubFromPaymentAccount() }
            step("Assert bank off-ramp options are requested") {
                assertTangemPayRequestSent("GET", bankOfframpOptionsEndpoint, optionsRequestsBeforeSecondOpen)
            }
            step("Click on 'To business account' button") { openTangemPayBankWithdrawToBusinessAccount() }
            step("Assert '$usdc' on '$polygon' is the default source") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { sourceRowWith(usdc, polygon).assertIsDisplayed() } }
            }
            step("Open the source token selector") { openTangemPayBankWithdrawSourceSelector() }
            step("Assert '$usdc' on '$polygon' and on '$arbitrum' are listed") {
                onTangemPayBankWithdrawAmountScreen {
                    selectedTokenRow(usdc, "$polygon · $polygonBalance").assertIsDisplayed()
                    tokenRow(usdc, "$arbitrum · $arbitrumBalance").assertIsDisplayed()
                }
            }
        }
    }
}