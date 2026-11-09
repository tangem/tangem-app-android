package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TangemPayBankAccounts.ACME
import com.tangem.common.constants.TangemPayBankAccounts.EMILY
import com.tangem.common.constants.TangemPayBankRoutes
import com.tangem.common.constants.TestConstants.PROVIDERS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.tapBackButton
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.TangemPayBankWithdrawField
import com.tangem.screens.tangempay.onTangemPayBankWithdrawAmountScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawChoiceScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawFormScreen
import com.tangem.screens.tangempay.onTangemPayBankWithdrawPreviewScreen
import com.tangem.screens.tangempay.onTangemPayWithdrawHubScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class MoneyAccountBankWithdrawTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val achRoute = TangemPayBankRoutes.ACH.copy(fee = "$1.51")
    private val wireRoute = TangemPayBankRoutes.WIRE.copy(fee = "$15.01")
    private val usdc = "USDC"
    private val polygon = "Polygon"
    private val toPersonalAccountTitle = getResourceString(CoreResR.string.tangempay_withdraw_hub_to_personal_account)
    private val accountTypeTitle = getResourceString(CoreResR.string.tangempay_withdraw_bank_account_type_title)
    private val transferTypeTitle = getResourceString(CoreResR.string.tangempay_withdraw_bank_transfer_type_title)
    private val ownAccount = getResourceString(CoreResR.string.tangempay_withdraw_bank_transfer_type_own)
    private val thirdPartyAccount = getResourceString(CoreResR.string.tangempay_withdraw_bank_transfer_type_third_party)
    private val checkingAccount = getResourceString(CoreResR.string.tangempay_withdraw_bank_account_type_checking)
    private val savingAccount = getResourceString(CoreResR.string.tangempay_withdraw_bank_account_type_saving)
    private val notEnoughFundsError = getResourceString(CoreResR.string.tangempay_withdraw_bank_amount_not_enough_funds)

    @AllureId("13262")
    @DisplayName("Money account: the bank transfer from the account offers only USDC on Polygon as the source")
    @Test
    fun bankTransferSourceIsAccountUsdcOnPolygonTest() {
        val swapSectionSubtitle = getResourceString(CoreResR.string.tangempay_withdraw_hub_swap_section_subtitle)
        val bankSectionSubtitle = getResourceString(CoreResR.string.tangempay_withdraw_hub_bank_section_subtitle)
        val firstStep = getResourceString(CoreResR.string.tangempay_withdraw_bank_step_counter, 1, 5)
        val usdAccount = getResourceString(CoreResR.string.money_account_main_name)
        val balance = "$25"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Assert 'Bank transfer' section is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
            }
            step("Assert '$bankSectionSubtitle' subtitle is displayed") {
                onTangemPayWithdrawHubScreen { hubText(bankSectionSubtitle).assertIsDisplayed() }
            }
            step("Assert '$swapSectionSubtitle' subtitle is not displayed") {
                onTangemPayWithdrawHubScreen { hubText(swapSectionSubtitle).assertDoesNotExist() }
            }
            step("Assert 'To personal account' button is displayed") {
                onTangemPayWithdrawHubScreen { toPersonalAccountButton.assertIsDisplayed() }
            }
            step("Assert 'To business account' button is displayed") {
                onTangemPayWithdrawHubScreen { toBusinessAccountButton.assertIsDisplayed() }
            }
            step("Assert bank recipients zero state is displayed") {
                onTangemPayWithdrawHubScreen { bankRecentsEmpty.assertIsDisplayed() }
            }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert '$toPersonalAccountTitle' header is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { headerTitle(toPersonalAccountTitle).assertIsDisplayed() } }
            }
            step("Assert '$firstStep' step counter is displayed") {
                onTangemPayBankWithdrawAmountScreen { headerTitle(firstStep).assertIsDisplayed() }
            }
            step("Assert '$usdAccount' source account is displayed") {
                onTangemPayBankWithdrawAmountScreen { headerTitle(usdAccount).assertIsDisplayed() }
            }
            step("Assert '$usdc' on '$polygon' source with balance '$balance' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen {
                        sourceRowWithBalance(usdc, polygon, balance).assertIsDisplayed()
                    }
                }
            }
            step("Assert source row does not open a token selection") {
                onTangemPayBankWithdrawAmountScreen { sourceRow.assertHasNoClickAction() }
            }
        }
    }

    @AllureId("13373")
    @DisplayName("Money account: the bank transfer amount checks Max, the limits and the fee with the network fee")
    @Test
    fun bankTransferAmountValidationTest() {
        val balanceScenario = "money_account_usdc_balance"
        val balanceEightState = "Eight"
        val networkFeeScenario = "money_account_network_fee"
        val networkFeeUnreachableState = "Unreachable"
        val networkFeeAvailableState = "Started"
        val balance = "$8"
        val networkFeeNote = getResourceString(CoreResR.string.money_account_send_network_fee_note, "$0.01")
        val outOfRangeError = getResourceString(CoreResR.string.tangempay_withdraw_bank_amount_error_range, "$1", "$10,000")
        val belowMinAmount = "0.5"
        val amount = "1"
        val wireMaxAmount = "8"
        val achMaxAmount = "6.49"
        val payTotal = "$2.51"
        val providersPolygonMockOnlyState = "PolygonMockOnly"

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(PROVIDERS_API_SCENARIO, providersPolygonMockOnlyState)
            },
        ).run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$balanceScenario' to '$balanceEightState'") {
                setWireMockScenarioState(balanceScenario, balanceEightState)
            }
            val routeCountBefore = countMoneyAccountBankWithdrawRoutes()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert '$usdc' on '$polygon' source with balance '$balance' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { sourceRowWithBalance(usdc, polygon, balance).assertIsDisplayed() }
                }
            }
            step("Assert 'Max' button is displayed") {
                onTangemPayBankWithdrawAmountScreen { maxButton.assertIsDisplayed() }
            }
            step("Assert '${achRoute.title}' route with fee '${achRoute.fee}' is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { routeRow(achRoute).assertIsDisplayed() } }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsNotEnabled() }
            }
            step("Open 'Select route' sheet") { openTangemPayBankWithdrawRouteSelector() }
            step("Assert '${achRoute.title}' route with fee '${achRoute.fee}' is displayed") {
                onTangemPayBankWithdrawAmountScreen { routeOption(achRoute).assertIsDisplayed() }
            }
            step("Assert '${wireRoute.title}' route with fee '${wireRoute.fee}' is displayed") {
                onTangemPayBankWithdrawAmountScreen { routeOption(wireRoute).assertIsDisplayed() }
            }
            step("Assert '$networkFeeNote' note is displayed") {
                onTangemPayBankWithdrawAmountScreen { routeSheetNote(networkFeeNote).assertIsDisplayed() }
            }
            step("Close 'Select route' sheet") { closeTangemPayBankWithdrawRouteSelector() }
            step("Enter amount '$belowMinAmount'") { enterTangemPayBankWithdrawAmount(belowMinAmount) }
            step("Assert '$outOfRangeError' error is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountError(outOfRangeError).assertIsDisplayed() } }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsNotEnabled() }
            }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Choose '${wireRoute.title}' route") { chooseTangemPayBankWithdrawRoute(wireRoute) }
            step("Assert '$notEnoughFundsError' error is displayed") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountError(notEnoughFundsError).assertIsDisplayed() } }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertIsNotEnabled() }
            }
            step("Click on 'Max' button") { clickTangemPayBankWithdrawMax() }
            step("Assert amount is '$wireMaxAmount'") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountInput.assertTextContains(wireMaxAmount) } }
            }
            step("Assert '$notEnoughFundsError' error is displayed") {
                onTangemPayBankWithdrawAmountScreen { amountError(notEnoughFundsError).assertIsDisplayed() }
            }
            step("Choose '${achRoute.title}' route") { chooseTangemPayBankWithdrawRoute(achRoute) }
            step("Click on 'Max' button") { clickTangemPayBankWithdrawMax() }
            step("Assert amount is '$achMaxAmount'") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { amountInput.assertTextContains(achMaxAmount) } }
            }
            step("Assert '$notEnoughFundsError' error is not displayed") {
                onTangemPayBankWithdrawAmountScreen { amountError(notEnoughFundsError).assertDoesNotExist() }
            }
            step("Assert 'Continue' button is enabled") {
                flakySafely { onTangemPayBankWithdrawAmountScreen { continueButton.assertIsEnabled() } }
            }
            step("Click on 'Back' button") { tapBackButton() }
            step("Assert 'Send' hub screen is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { bankSectionTitle.assertIsDisplayed() } }
            }
            step("Set WireMock scenario '$networkFeeScenario' to '$networkFeeUnreachableState'") {
                setWireMockScenarioState(networkFeeScenario, networkFeeUnreachableState)
            }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert network fee unreachable message is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { networkFeeUnreachable.assertIsDisplayed() }
                }
            }
            step("Assert 'Continue' button is not displayed") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertDoesNotExist() }
            }
            val feeEstimatesBefore = countMoneyAccountNetworkFeeEstimates()
            step("Click on 'Refresh' button") {
                onTangemPayBankWithdrawAmountScreen { networkFeeRefreshButton.performClick() }
            }
            step("Assert the network fee is requested again") {
                flakySafely { assertTrue(countMoneyAccountNetworkFeeEstimates() > feeEstimatesBefore) }
            }
            step("Assert network fee unreachable message is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { networkFeeUnreachable.assertIsDisplayed() }
                }
            }
            step("Assert 'Continue' button is not displayed") {
                onTangemPayBankWithdrawAmountScreen { continueButton.assertDoesNotExist() }
            }
            step("Set WireMock scenario '$networkFeeScenario' to '$networkFeeAvailableState'") {
                setWireMockScenarioState(networkFeeScenario, networkFeeAvailableState)
            }
            step("Refresh the network fee until '${achRoute.title}' route is displayed") {
                refreshTangemPayBankWithdrawNetworkFeeUntilRoute(achRoute)
            }
            step("Assert '$usdc' on '$polygon' source with balance '$balance' is displayed") {
                onTangemPayBankWithdrawAmountScreen { sourceRowWithBalance(usdc, polygon, balance).assertIsDisplayed() }
            }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Fill in '$ownAccount' '$checkingAccount' recipient '${EMILY.fullName}'") {
                fillTangemPayNewBankRecipient(ownAccount, checkingAccount, EMILY)
            }
            step("Assert 'Pay $payTotal' button is displayed") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { payButton(payTotal).assertIsDisplayed() } }
            }
            step("Click on 'Back' button") { tapBackButton() }
            step("Assert bank name '${EMILY.bankName}' is kept") {
                flakySafely {
                    onTangemPayBankWithdrawFormScreen {
                        input(TangemPayBankWithdrawField.BankName).assertTextContains(EMILY.bankName)
                    }
                }
            }
            step("Assert no bank-offramp route is created") {
                assertEquals(routeCountBefore, countMoneyAccountBankWithdrawRoutes())
            }
        }
    }

    @AllureId("13374")
    @DisplayName("Money account: a new bank recipient is added through the form up to the confirmation")
    @Test
    fun newBankRecipientThroughFormTest() {
        val amount = "1"
        val balance = "$25"
        val payTotal = "$2.51"
        val invalidRoutingNumber = "12345"
        val routingNumberError = getResourceString(CoreResR.string.tangempay_withdraw_bank_field_error_routing_number)
        val feesFooter = getResourceString(CoreResR.string.money_account_send_fees_footer, achRoute.title)
        val recipientFields = listOf(
            TangemPayBankWithdrawField.AccountNumber,
            TangemPayBankWithdrawField.RoutingNumber,
            TangemPayBankWithdrawField.FirstName,
            TangemPayBankWithdrawField.LastName,
            TangemPayBankWithdrawField.Country,
            TangemPayBankWithdrawField.City,
            TangemPayBankWithdrawField.AddressLine1,
            TangemPayBankWithdrawField.PostalCode,
        )
        val bankFields = listOf(
            TangemPayBankWithdrawField.BankName,
            TangemPayBankWithdrawField.BankRegion,
            TangemPayBankWithdrawField.BankCity,
            TangemPayBankWithdrawField.BankAddressLine1,
            TangemPayBankWithdrawField.BankPostalCode,
        )

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            val routeCountBefore = countMoneyAccountBankWithdrawRoutes()
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert '$transferTypeTitle' step is displayed") {
                assertTangemPayBankWithdrawChoiceStepOpened(transferTypeTitle)
            }
            listOf(ownAccount, thirdPartyAccount).forEach { option ->
                step("Assert '$option' option is displayed") {
                    onTangemPayBankWithdrawChoiceScreen { option(option).assertIsDisplayed() }
                }
            }
            step("Choose '$ownAccount' option") { chooseTangemPayBankWithdrawOption(transferTypeTitle, ownAccount) }
            step("Assert '$accountTypeTitle' step is displayed") {
                assertTangemPayBankWithdrawChoiceStepOpened(accountTypeTitle)
            }
            listOf(checkingAccount, savingAccount).forEach { option ->
                step("Assert '$option' option is displayed") {
                    onTangemPayBankWithdrawChoiceScreen { option(option).assertIsDisplayed() }
                }
            }
            step("Choose '$checkingAccount' option") { chooseTangemPayBankWithdrawOption(accountTypeTitle, checkingAccount) }
            recipientFields.forEach { field ->
                step("Assert '${field.name}' field is displayed") {
                    flakySafely {
                        onTangemPayBankWithdrawFormScreen {
                            field(field).performScrollTo()
                            field(field).assertIsDisplayed()
                        }
                    }
                }
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawFormScreen { continueButton.assertIsNotEnabled() }
            }
            step("Enter account number '${EMILY.accountNumber}'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.AccountNumber, EMILY.accountNumber)
            }
            step("Assert '$routingNumberError' is displayed for routing number '$invalidRoutingNumber'") {
                assertTangemPayBankWithdrawFieldError(
                    field = TangemPayBankWithdrawField.RoutingNumber,
                    value = invalidRoutingNumber,
                    blurField = TangemPayBankWithdrawField.FirstName,
                    error = routingNumberError,
                )
            }
            step("Assert 'Continue' button is disabled") {
                onTangemPayBankWithdrawFormScreen { continueButton.assertIsNotEnabled() }
            }
            step("Assert '$routingNumberError' is not displayed for routing number '${EMILY.routingNumber}'") {
                assertTangemPayBankWithdrawFieldValid(
                    field = TangemPayBankWithdrawField.RoutingNumber,
                    value = EMILY.routingNumber,
                    blurField = TangemPayBankWithdrawField.FirstName,
                    error = routingNumberError,
                )
            }
            step("Enter first name '${EMILY.firstName}'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.FirstName, EMILY.firstName)
            }
            step("Enter last name '${EMILY.lastName}'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.LastName, EMILY.lastName)
            }
            step("Choose country '${EMILY.country}' and state '${EMILY.region}'") {
                selectTangemPayBankWithdrawResidency(TangemPayBankWithdrawField.Country, EMILY.country, EMILY.region)
            }
            step("Assert region '${EMILY.region}' is displayed") {
                flakySafely {
                    onTangemPayBankWithdrawFormScreen {
                        valueOf(TangemPayBankWithdrawField.Region, EMILY.region).assertIsDisplayed()
                    }
                }
            }
            step("Click on 'Back' button") { tapBackButton() }
            step("Assert '$accountTypeTitle' step is displayed") {
                flakySafely { onTangemPayBankWithdrawChoiceScreen { sectionTitle(accountTypeTitle).assertIsDisplayed() } }
            }
            step("Click on 'Continue' button") {
                flakySafely { onTangemPayBankWithdrawChoiceScreen { continueButton.assertIsEnabled() } }
                onTangemPayBankWithdrawChoiceScreen { continueButton.performClick() }
            }
            step("Assert account number '${EMILY.accountNumber}' is kept") {
                flakySafely {
                    onTangemPayBankWithdrawFormScreen {
                        input(TangemPayBankWithdrawField.AccountNumber).assertTextContains(EMILY.accountNumber)
                    }
                }
            }
            step("Assert first name '${EMILY.firstName}' is kept") {
                onTangemPayBankWithdrawFormScreen {
                    input(TangemPayBankWithdrawField.FirstName).assertTextContains(EMILY.firstName)
                }
            }
            step("Assert country '${EMILY.country}' is kept") {
                onTangemPayBankWithdrawFormScreen {
                    valueOf(TangemPayBankWithdrawField.Country, EMILY.country).assertIsDisplayed()
                }
            }
            step("Enter city '${EMILY.city}'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.City, EMILY.city)
            }
            step("Enter address '${EMILY.addressLine1}'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.AddressLine1, EMILY.addressLine1)
            }
            step("Enter postal code '${EMILY.postalCode}'") {
                enterTangemPayBankWithdrawField(TangemPayBankWithdrawField.PostalCode, EMILY.postalCode)
            }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawFormContinue() }
            bankFields.forEach { field ->
                step("Assert '${field.name}' field is displayed") {
                    flakySafely {
                        onTangemPayBankWithdrawFormScreen {
                            field(field).performScrollTo()
                            field(field).assertIsDisplayed()
                        }
                    }
                }
            }
            step("Fill in bank details of '${EMILY.bankName}'") { fillTangemPayBankDetails(EMILY) }
            step("Assert 'Confirm transfer' screen is displayed") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { title.assertIsDisplayed() } }
            }
            step("Assert amount '$$amount' and balance '$balance' are displayed") {
                onTangemPayBankWithdrawPreviewScreen { amountCard("$$amount", balance).assertIsDisplayed() }
            }
            step("Assert '${achRoute.title}' route with fee '${achRoute.fee}' is displayed") {
                onTangemPayBankWithdrawPreviewScreen { routeRow(achRoute).assertIsDisplayed() }
            }
            listOf(
                EMILY.accountNumber,
                EMILY.routingNumber,
                EMILY.firstName,
                EMILY.lastName,
                EMILY.country,
                EMILY.city,
                EMILY.addressLine1,
                EMILY.postalCode,
            ).forEach { value ->
                step("Assert recipient info '$value' is displayed") {
                    onTangemPayBankWithdrawPreviewScreen { recipientCard(value).assertIsDisplayed() }
                }
            }
            step("Assert bank name '${EMILY.bankName}' is displayed") {
                onTangemPayBankWithdrawPreviewScreen {
                    detailValue(EMILY.bankName).performScrollTo()
                    detailValue(EMILY.bankName).assertIsDisplayed()
                }
            }
            step("Assert '$feesFooter' footer is displayed") {
                onTangemPayBankWithdrawPreviewScreen {
                    detailValue(feesFooter).performScrollTo()
                    detailValue(feesFooter).assertIsDisplayed()
                }
            }
            step("Assert 'Pay $payTotal' button is displayed") {
                onTangemPayBankWithdrawPreviewScreen { payButton(payTotal).assertIsDisplayed() }
            }
            step("Assert no bank-offramp route is created") {
                assertEquals(routeCountBefore, countMoneyAccountBankWithdrawRoutes())
            }
            step("Close 'Confirm transfer' screen") { closeMoneyAccountBankWithdrawPreview() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Assert bank recipients zero state is displayed") {
                flakySafely { onTangemPayWithdrawHubScreen { bankRecentsEmpty.assertIsDisplayed() } }
            }
        }
    }

    @AllureId("13364")
    @DisplayName("Money account: the bank transfer offers ACH and Wire requested by the banking account type")
    @Test
    fun bankTransferOffersAchAndWireTest() {
        val bankAccountsScenario = "tangem_pay_bank_accounts"
        val twoAccountsState = "TwoAccounts"
        val accountTypeParam = "account_type"
        val bankingAccountType = "BANKING_ACCOUNT"
        val productInstanceParam = "product_instance_id"
        val paymentAccountType = "PAYMENT_ACCOUNT"
        val amount = "1"
        val payTotal = "$16.01"

        setupHooks().run {
            step("Set WireMock scenarios for a Money account bank transfer") { setMoneyAccountBankWithdrawScenarios() }
            step("Set WireMock scenario '$bankAccountsScenario' to '$twoAccountsState'") {
                setWireMockScenarioState(bankAccountsScenario, twoAccountsState)
            }
            val optionsCountBefore = countTangemPayBankOfframpOptionsRequests()
            val bankingOptionsCountBefore = countTangemPayBankOfframpOptionsRequests(accountTypeParam, bankingAccountType)
            val productOptionsCountBefore = countTangemPayBankOfframpOptionsRequests(productInstanceParam)
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Open 'Send' hub") { openMoneyAccountSendHub() }
            step("Click on 'To personal account' button") { openTangemPayBankWithdrawToPersonalAccount() }
            step("Assert the bank-offramp options are requested with '$accountTypeParam=$bankingAccountType' only") {
                val optionsSent = countTangemPayBankOfframpOptionsRequests() - optionsCountBefore
                val bankingOptionsSent = countTangemPayBankOfframpOptionsRequests(accountTypeParam, bankingAccountType) -
                    bankingOptionsCountBefore
                assertTrue("The options request should be sent", optionsSent > 0)
                assertEquals("Every options request should ask for $bankingAccountType", optionsSent, bankingOptionsSent)
            }
            step("Assert the bank-offramp options are requested without '$productInstanceParam'") {
                assertEquals(productOptionsCountBefore, countTangemPayBankOfframpOptionsRequests(productInstanceParam))
            }
            step("Open 'Select route' sheet") { openTangemPayBankWithdrawRouteSelector() }
            step("Assert '${achRoute.title}' route with fee '${achRoute.fee}' is displayed") {
                onTangemPayBankWithdrawAmountScreen { routeOption(achRoute).assertIsDisplayed() }
            }
            step("Assert '${wireRoute.title}' route with fee '${wireRoute.fee}' is displayed") {
                onTangemPayBankWithdrawAmountScreen { routeOption(wireRoute).assertIsDisplayed() }
            }
            step("Close 'Select route' sheet") { closeTangemPayBankWithdrawRouteSelector() }
            step("Click on 'Back' button") { tapBackButton() }
            step("Open recent recipient '${ACME.shortName}'") { openTangemPayRecentRecipient(ACME) }
            step("Assert '${wireRoute.title}' route with fee '${wireRoute.fee}' is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayBankWithdrawAmountScreen { routeRow(wireRoute).assertIsDisplayed() }
                }
            }
            step("Enter amount '$amount'") { enterTangemPayBankWithdrawAmount(amount) }
            step("Click on 'Continue' button") { clickTangemPayBankWithdrawAmountContinue() }
            step("Assert '${wireRoute.title}' route with fee '${wireRoute.fee}' is displayed on 'Confirm transfer'") {
                flakySafely { onTangemPayBankWithdrawPreviewScreen { routeRow(wireRoute).assertIsDisplayed() } }
            }
            step("Assert 'Pay $payTotal' button is displayed") {
                onTangemPayBankWithdrawPreviewScreen { payButton(payTotal).assertIsDisplayed() }
            }
            step("Close 'Confirm transfer' screen") { closeMoneyAccountBankWithdrawPreview() }
            step("Return to 'Main' screen") { returnToMainFromMoneyAccount() }
            val paymentOptionsCountBefore = countTangemPayBankOfframpOptionsRequests(accountTypeParam, paymentAccountType)
            step("Click on Tangem Pay tile") { clickTangemPayTileUntilPaymentAccountOpens() }
            step("Open 'Withdraw' hub of the payment account") { openWithdrawHubFromPaymentAccount() }
            step("Assert the payment account requests the options with '$accountTypeParam=$paymentAccountType'") {
                flakySafely {
                    assertTrue(
                        countTangemPayBankOfframpOptionsRequests(accountTypeParam, paymentAccountType) >
                            paymentOptionsCountBefore,
                    )
                }
            }
        }
    }
}