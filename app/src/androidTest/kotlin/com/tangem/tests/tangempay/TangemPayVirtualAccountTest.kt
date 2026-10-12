package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertTextSharedViaChooser
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubShareChooserIntent
import com.tangem.scenarios.*
import com.tangem.screens.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TangemPayVirtualAccountTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val channelsVirtualAccountState = "VirtualAccount"
    private val balanceScenario = "tangem_pay_balance_update"
    private val balanceInitialState = "InitialBalance"
    private val historyScenario = "tangem_pay_transaction_history"
    private val historyInitialState = "InitialEmpty"

    private val feesScenario = "tangem_pay_onramp_fees"
    private val feesBothState = "Started"
    private val feesAchOnlyState = "AchOnly"
    private val feesFedwireOnlyState = "FedwireOnly"
    private val feesUnavailableState = "Unavailable"

    private val orderScenario = "tangem_pay_virtual_account_order"
    private val orderProcessingState = "Started"
    private val orderCompletedState = "Completed"
    private val accountScenario = "tangem_pay_virtual_account"
    private val accountActiveState = "AccountActive"

    // The mocked build short-circuits the create-order call, so the order is only observable through its poll.
    private val orderPollEndpointPattern = "/bff-v2/v1/order/mock-va-order-id"

    // Android re-resolves the virtual-account on-ramp from /customer/me; the order poll only triggers that refetch.
    private val customerInfoEndpointPattern = "/bff-v2/v1/customer/me"

    private val achFeeType = "ACH_ONRAMP"
    private val fedwireFeeType = "FEDWIRE_ONRAMP"

    // Fee titles and amounts come from the backend response, not from string resources.
    private val achFeeTitle = "ACH"
    private val achFeeValue = "$1.50"
    private val fedwireFeeTitle = "FedWire"
    private val fedwireFeeValue = "$25"

    private val bankCredentialsScenario = "tangem_pay_bank_credentials"
    private val bankCredentialsSuccessState = "Started"
    private val bankCredentialsErrorState = "Error"

    private val bankCredentialsEndpointPattern = "/bff-v2/v1/account/bank-credentials/mock-va-product-id"

    private val expectedBeneficiaryName = "Hot Wallet User"
    private val expectedBeneficiaryAddress = "1 Market St, San Francisco, CA 94105"
    private val expectedBankAddress = "1801 Main St, Kansas City, MO 64108"
    private val expectedAccountNumber = "1234567890"
    private val expectedRoutingNumber = "101019644"
    private val expectedBankName = "Lead Bank"

    // The deposit limit is a placeholder hardcoded in the app ([REDACTED_TASK_KEY]), so it has no string resource.
    private val expectedDepositLimit = "Available to deposit per day: $10,000"
    private val expectedDepositLimitHint = "Limit is resetting every day"

    @AllureId("10292")
    @DisplayName("Tangem Pay: Add funds opens a virtual account and reveals banking details once the order completes")
    @Test
    fun openVirtualAccountViaAddFundsTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(feesScenario, feesBothState)
                setWireMockScenarioState(orderScenario, orderProcessingState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open bank transfer sheet from 'Add funds'") { openTangemPayBankTransferSheet() }
            step("Assert '$achFeeTitle' fee is displayed") {
                assertVirtualAccountFee(achFeeType, achFeeTitle, achFeeValue)
            }
            step("Assert '$fedwireFeeTitle' fee is displayed") {
                assertVirtualAccountFee(fedwireFeeType, fedwireFeeTitle, fedwireFeeValue)
            }
            val orderPollsBefore = getWireMockRequestCount("GET", orderPollEndpointPattern)
            step("Click on 'Show details' button") {
                onTangemPayVirtualAccountDepositSheet { showDetailsButton.clickWithAssertion() }
            }
            step("Assert 'Preparing your banking details' screen is displayed") {
                onTangemPayVirtualAccountSuccessScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { title.assertIsDisplayed() }
                }
            }
            step("Assert the virtual account order is being polled") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    val polls = getWireMockRequestCount("GET", orderPollEndpointPattern) - orderPollsBefore
                    if (polls <= 0) throw AssertionError("Creating the order should start polling it")
                }
            }
            step("Click on 'Close' button") {
                onTangemPayVirtualAccountSuccessScreen { closeButton.clickWithAssertion() }
            }
            step("Assert payment account balance is displayed") {
                awaitSuccess { onTangemPayMainScreen { balance.assertIsDisplayed() } }
            }
            // The activated account goes first: the poller refetches customer info the moment it sees COMPLETED,
            // and that response is what flips the on-ramp to Available.
            step("Switch WireMock scenario '$accountScenario' to '$accountActiveState'") {
                setWireMockScenarioState(accountScenario, accountActiveState)
            }
            val customerInfoBefore = getWireMockRequestCount("GET", customerInfoEndpointPattern)
            step("Switch WireMock scenario '$orderScenario' to '$orderCompletedState'") {
                setWireMockScenarioState(orderScenario, orderCompletedState)
            }
            step("Wait until the completed order refetches customer info") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    val reloads = getWireMockRequestCount("GET", customerInfoEndpointPattern) - customerInfoBefore
                    if (reloads <= 0) throw AssertionError("Completed order should refetch customer info")
                }
                waitForIdle()
            }
            step("Open bank transfer sheet from 'Add funds'") { openTangemPayBankTransferSheet() }
            step("Click on 'Show details' button") {
                onTangemPayVirtualAccountDepositSheet { showDetailsButton.clickWithAssertion() }
            }
            step("Assert virtual account banking details are displayed") {
                onVirtualAccountRequisitesBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        accountNumber.assertTextEquals(expectedAccountNumber)
                    }
                    routingNumber.assertTextEquals(expectedRoutingNumber)
                    bankName.assertTextEquals(expectedBankName)
                }
            }
        }
    }

    @AllureId("10871")
    @DisplayName("Tangem Pay: bank transfer sheet shows both ACH and FedWire fees")
    @Test
    fun bankTransferSheetShowsBothFeesTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(feesScenario, feesBothState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open bank transfer sheet from 'Add funds'") { openTangemPayBankTransferSheet() }
            step("Assert '$achFeeTitle' fee is displayed") {
                assertVirtualAccountFee(achFeeType, achFeeTitle, achFeeValue)
            }
            step("Assert '$fedwireFeeTitle' fee is displayed") {
                assertVirtualAccountFee(fedwireFeeType, fedwireFeeTitle, fedwireFeeValue)
            }
        }
    }

    @AllureId("10872")
    @DisplayName("Tangem Pay: bank transfer sheet hides FedWire when the backend returns ACH only")
    @Test
    fun bankTransferSheetShowsAchOnlyTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(feesScenario, feesAchOnlyState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open bank transfer sheet from 'Add funds'") { openTangemPayBankTransferSheet() }
            step("Assert '$achFeeTitle' fee is displayed") {
                assertVirtualAccountFee(achFeeType, achFeeTitle, achFeeValue)
            }
            step("Assert '$fedwireFeeTitle' fee is not displayed") {
                assertVirtualAccountFeeNotDisplayed(fedwireFeeType, fedwireFeeTitle)
            }
        }
    }

    @AllureId("10873")
    @DisplayName("Tangem Pay: bank transfer sheet hides ACH when the backend returns FedWire only")
    @Test
    fun bankTransferSheetShowsFedwireOnlyTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(feesScenario, feesFedwireOnlyState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open bank transfer sheet from 'Add funds'") { openTangemPayBankTransferSheet() }
            step("Assert '$fedwireFeeTitle' fee is displayed") {
                assertVirtualAccountFee(fedwireFeeType, fedwireFeeTitle, fedwireFeeValue)
            }
            step("Assert '$achFeeTitle' fee is not displayed") {
                assertVirtualAccountFeeNotDisplayed(achFeeType, achFeeTitle)
            }
        }
    }

    @AllureId("11295")
    @DisplayName("Tangem Pay: bank transfer fee error banner reloads the fees on tap")
    @Test
    fun bankTransferSheetReloadsFeesFromErrorBannerTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(feesScenario, feesUnavailableState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open bank transfer sheet from 'Add funds'") { openTangemPayBankTransferSheet() }
            step("Assert fee error banner is displayed") {
                onTangemPayVirtualAccountDepositSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { feeErrorTitle.assertIsDisplayed() }
                }
            }
            step("Assert '$achFeeTitle' fee is not displayed") {
                assertVirtualAccountFeeNotDisplayed(achFeeType, achFeeTitle)
            }
            step("Assert '$fedwireFeeTitle' fee is not displayed") {
                assertVirtualAccountFeeNotDisplayed(fedwireFeeType, fedwireFeeTitle)
            }
            step("Switch WireMock scenario '$feesScenario' to '$feesBothState'") {
                setWireMockScenarioState(feesScenario, feesBothState)
            }
            step("Click on fee error banner") {
                onTangemPayVirtualAccountDepositSheet { feeErrorBanner.clickWithAssertion() }
            }
            step("Assert '$achFeeTitle' fee is displayed") {
                assertVirtualAccountFee(achFeeType, achFeeTitle, achFeeValue)
            }
            step("Assert '$fedwireFeeTitle' fee is displayed") {
                assertVirtualAccountFee(fedwireFeeType, fedwireFeeTitle, fedwireFeeValue)
            }
        }
    }

    @AllureId("10295")
    @DisplayName("Tangem Pay: virtual account details list every requisite, the deposit limit, copy and share")
    @Test
    fun virtualAccountRequisitesAreCopiedAndSharedTest() {
        val expectedSharedText = listOf(
            VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_NAME to expectedBeneficiaryName,
            VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_ADDRESS to expectedBeneficiaryAddress,
            VirtualAccountRequisitesBottomSheetPageObject.BANK_NAME to expectedBankName,
            VirtualAccountRequisitesBottomSheetPageObject.BANK_ADDRESS to expectedBankAddress,
            VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER to expectedAccountNumber,
            VirtualAccountRequisitesBottomSheetPageObject.ROUTING_NUMBER to expectedRoutingNumber,
        ).joinToString(separator = "") { (field, value) -> "$field: $value\n" }

        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(feesScenario, feesBothState)
                setWireMockScenarioState(accountScenario, accountActiveState)
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsSuccessState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Stub the system share chooser") { stubShareChooserIntent() }
            step("Open the virtual account banking details") { openTangemPayVirtualAccountRequisites() }
            step("Assert every banking detail matches the bank-credentials response") {
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
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER,
                    expectedValue = expectedAccountNumber,
                )
                assertVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ROUTING_NUMBER,
                    expectedValue = expectedRoutingNumber,
                )
            }
            step("Assert the daily deposit limit block is displayed") {
                onVirtualAccountRequisitesBottomSheet {
                    depositLimitTitle.performScrollTo()
                    depositLimitTitle.assertTextEquals(expectedDepositLimit)
                    depositLimitSubtitle.assertTextEquals(expectedDepositLimitHint)
                }
            }
            step("Copy every banking detail and assert it reaches the clipboard") {
                copyVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_NAME,
                    expectedValue = expectedBeneficiaryName,
                )
                copyVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BENEFICIARY_ADDRESS,
                    expectedValue = expectedBeneficiaryAddress,
                )
                copyVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BANK_NAME,
                    expectedValue = expectedBankName,
                )
                copyVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.BANK_ADDRESS,
                    expectedValue = expectedBankAddress,
                )
                copyVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ACCOUNT_NUMBER,
                    expectedValue = expectedAccountNumber,
                )
                copyVirtualAccountRequisite(
                    field = VirtualAccountRequisitesBottomSheetPageObject.ROUTING_NUMBER,
                    expectedValue = expectedRoutingNumber,
                )
            }
            step("Click on 'Share' button") {
                onVirtualAccountRequisitesBottomSheet {
                    shareButton.performScrollTo()
                    shareButton.performClick()
                }
            }
            step("Assert the banking details are passed to the system share chooser") {
                assertTextSharedViaChooser(expectedSharedText, times = 1)
            }
        }
    }

    @AllureId("10297")
    @DisplayName("Tangem Pay: a failed bank-credentials request shows the error state and Retry loads the details")
    @Test
    fun bankingDetailsErrorRetryLoadsRequisitesTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
                setWireMockScenarioState(balanceScenario, balanceInitialState)
                setWireMockScenarioState(historyScenario, historyInitialState)
                setWireMockScenarioState(feesScenario, feesBothState)
                setWireMockScenarioState(accountScenario, accountActiveState)
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsErrorState)
            }
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open bank transfer sheet from 'Add funds'") { openTangemPayBankTransferSheet() }
            step("Click on 'Show details' button") {
                onTangemPayVirtualAccountDepositSheet { showDetailsButton.clickWithAssertion() }
            }
            step("Assert the banking details error state is displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayVaBankingDetailsErrorSheet { title.assertIsDisplayed() }
                }
                onTangemPayVaBankingDetailsErrorSheet {
                    icon.assertIsDisplayed()
                    description.assertIsDisplayed()
                    contactSupportButton.assertIsDisplayed()
                    retryButton.assertIsDisplayed()
                }
            }
            step("Switch WireMock scenario '$bankCredentialsScenario' to '$bankCredentialsSuccessState'") {
                setWireMockScenarioState(bankCredentialsScenario, bankCredentialsSuccessState)
            }
            val credentialsRequestsBefore = getWireMockRequestCount("GET", bankCredentialsEndpointPattern)
            step("Click on 'Retry' button") {
                onTangemPayVaBankingDetailsErrorSheet { retryButton.clickWithAssertion() }
            }
            step("Assert the virtual account banking details are displayed") {
                awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) {
                    onVirtualAccountRequisitesBottomSheet {
                        accountNumber.assertTextEquals(expectedAccountNumber)
                    }
                }
                onVirtualAccountRequisitesBottomSheet {
                    routingNumber.assertTextEquals(expectedRoutingNumber)
                    bankName.assertTextEquals(expectedBankName)
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
}