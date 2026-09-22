package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertClipboardTextEquals
import com.tangem.common.utils.assertTextSharedViaChooser
import com.tangem.common.utils.getWireMockRequestCount
import com.tangem.common.utils.getWireMockRequestCountByQueryParam
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubShareChooserIntent
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Test

@HiltAndroidTest
class TangemPayMultichainDepositTest : BaseTestCase() {

    private val polygonNetwork = "Polygon"
    private val bscNetwork = "BNB Smart Chain"
    private val baseNetwork = "Base"
    private val arbitrumNetwork = "Arbitrum One"
    private val ethereumNetwork = "Ethereum"

    private val bscDepositAddress = "0x0000000000000000000000000000000000000056"
    private val baseDepositAddress = "0x0000000000000000000000000000000000008453"
    private val paymentTokensLabel = "USDC, USDT"

    // GET and POST share this path; the lookup is additionally narrowed by its order-type query parameter.
    private val orderEndpoint = "/bff-v2/v1/order"
    private val orderTypesQueryParam = "order_types"
    private val smartContractOrderType = "SMART_CONTRACT_ISSUE_RAIN"

    @AllureId("11366")
    @DisplayName("Tangem Pay: 'Add funds' lists bank transfer, then swap, then receive")
    @Test
    fun addFundsOptionsOrderTest() {
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val channelsVirtualAccountState = "VirtualAccount"
        // The bank-transfer option is only offered to a virtual-account customer, and this is the only
        // Android customer state that is one.
        val virtualAccountScenario = "tangem_pay_virtual_account"
        val virtualAccountActiveState = "AccountActive"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step(
                "Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO' " +
                    "to '$channelsVirtualAccountState'",
            ) {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
            }
            step("Set WireMock scenario '$virtualAccountScenario' to '$virtualAccountActiveState'") {
                setWireMockScenarioState(virtualAccountScenario, virtualAccountActiveState)
            }
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open the 'Add funds' bottom sheet") { openTangemPayAddFunds() }
            step("Assert 'From a bank account' is displayed as the first option") {
                onTangemPayAddFundsSheet {
                    optionAt(index = 0).assertTextContainsSafe(
                        getResourceString(CoreResR.string.tangempay_topup_bank_transfer_title),
                    )
                    optionAt(index = 0).assertTextContainsSafe(
                        getResourceString(CoreResR.string.tangempay_topup_bank_transfer_body),
                    )
                }
            }
            step("Assert 'From your Tangem Wallet' is displayed as the second option") {
                onTangemPayAddFundsSheet {
                    optionAt(index = 1).assertTextContainsSafe(
                        getResourceString(CoreResR.string.tangempay_topup_swap_title),
                    )
                    optionAt(index = 1).assertTextContainsSafe(
                        getResourceString(CoreResR.string.tangempay_topup_swap_body),
                    )
                }
            }
            step("Assert 'From another wallet or exchange' is displayed as the third option") {
                onTangemPayAddFundsSheet {
                    optionAt(index = 2).assertTextContainsSafe(
                        getResourceString(CoreResR.string.tangempay_topup_receive_title),
                    )
                    optionAt(index = 2).assertTextContainsSafe(
                        getResourceString(CoreResR.string.tangempay_topup_receive_body_multichain),
                    )
                }
            }
        }
    }

    @AllureId("11367")
    @DisplayName("Tangem Pay: a multichain account is shown in USD on the tile and in the account header")
    @Test
    fun paymentAccountMultiNetworkIndicatorsTest() {
        val aggregatedBalance = "17.00"
        val fiatCurrencyCode = "USD"

        setupHooks().run {
            step("Set WireMock scenarios for a multichain Tangem Pay account") {
                setTangemPayMultiNetworkScenarios()
            }
            step("Open the Main screen with the Tangem Pay tile") { openMainScreenWithTangemPayTile() }
            step("Assert the Tangem Pay tile balance '$aggregatedBalance' is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        tileBalance.assertTextContainsSafe(aggregatedBalance, substring = true)
                    }
                }
            }
            step("Assert the tile currency '$fiatCurrencyCode' is displayed") {
                onTangemPayMainScreen { tileWithSubtitle(fiatCurrencyCode).assertIsDisplayed() }
            }
            step("Click on Tangem Pay tile") {
                onTangemPayMainScreen { mainScreenTile.clickWithAssertion() }
            }
            step("Assert the payment account title and its fiat subtitle are displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { paymentAccountTitle.assertIsDisplayed() }
                    multinetworkSubtitle.assertIsDisplayed()
                }
            }
            step("Assert the payment account balance '$aggregatedBalance' is displayed") {
                onTangemPayMainScreen {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                        balance.assertTextContainsSafe(aggregatedBalance, substring = true)
                    }
                }
            }
        }
    }

    @AllureId("11368")
    @DisplayName("Tangem Pay: 'Choose network' splits the networks into fast and other options")
    @Test
    fun chooseNetworkSplitsFastAndOtherOptionsTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a multichain Tangem Pay account") {
                setTangemPayMultiNetworkScenarios()
            }
            step("Open the 'Choose network' bottom sheet") { openTangemPayChooseNetworkSheetFromMain() }
            step("Assert '$polygonNetwork', '$bscNetwork' and '$baseNetwork' are displayed under 'Fast options'") {
                onTangemPayChooseNetworkSheet {
                    fastOptionsTitle.assertIsDisplayed()
                    fastOptionNetwork(polygonNetwork).assertIsDisplayed()
                    fastOptionNetwork(bscNetwork).assertIsDisplayed()
                    fastOptionNetwork(baseNetwork).assertIsDisplayed()
                }
            }
            step("Assert '$arbitrumNetwork' and '$ethereumNetwork' are displayed under 'Other options'") {
                onTangemPayChooseNetworkSheet {
                    otherOptionsTitle.assertIsDisplayed()
                    otherOptionNetwork(arbitrumNetwork).assertIsDisplayed()
                    otherOptionNetwork(ethereumNetwork).assertIsDisplayed()
                }
            }
            step("Assert the '$bscNetwork' row subtitle '$paymentTokensLabel' is displayed") {
                onTangemPayChooseNetworkSheet {
                    networkSubtitle(bscNetwork).assertTextContainsSafe(paymentTokensLabel)
                }
            }
        }
    }

    @AllureId("11370")
    @DisplayName("Tangem Pay: a disabled network explains the EVM-chain workaround instead of an address")
    @Test
    fun disabledNetworkOpensOtherNetworksSheetTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a multichain Tangem Pay account") {
                setTangemPayMultiNetworkScenarios()
            }
            step("Open the 'Choose network' bottom sheet") { openTangemPayChooseNetworkSheetFromMain() }
            step("Click on the '$arbitrumNetwork' network") {
                onTangemPayChooseNetworkSheet { otherOptionNetwork(arbitrumNetwork).clickWithAssertion() }
            }
            step("Assert the 'Other networks' sheet is displayed") {
                onTangemPayOtherNetworksSheet {
                    flakySafely { header.assertIsDisplayed() }
                    title.assertIsDisplayed()
                    subtitle.assertIsDisplayed()
                }
            }
            step("Assert the deposit address is not displayed") {
                onTangemPayReceiveSheet { address.assertDoesNotExist() }
            }
            step("Click on 'Close' button") {
                onTangemPayOtherNetworksSheet { closeButton.clickWithAssertion() }
            }
            step("Assert the 'Choose network' sheet is displayed again") {
                onTangemPayChooseNetworkSheet {
                    flakySafely { fastOptionNetwork(polygonNetwork).assertIsDisplayed() }
                    fastOptionNetwork(bscNetwork).assertIsDisplayed()
                    fastOptionNetwork(baseNetwork).assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("11372")
    @DisplayName("Tangem Pay: an enabled network shows its deposit address and QR code")
    @Test
    fun enabledNetworkReceiveSheetShowsAddressAndQrCodeTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a multichain Tangem Pay account") {
                setTangemPayMultiNetworkScenarios()
            }
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Open the 'Add funds' bottom sheet") { openTangemPayAddFunds() }
            step("Open the receive sheet for the '$bscNetwork' network") {
                assertEquals(
                    "Unexpected deposit address",
                    bscDepositAddress,
                    openTangemPayReceiveSheet(bscNetwork),
                )
            }
            step("Assert the receive warning and the tokens-on-network heading are displayed") {
                onTangemPayReceiveSheet {
                    warningTitle(paymentTokensLabel, bscNetwork).assertIsDisplayed()
                    warningDescription.assertIsDisplayed()
                    tokensOnNetworkHeading(paymentTokensLabel, bscNetwork).assertIsDisplayed()
                }
            }
            step("Assert 'Copy', 'Share' and 'Show QR' buttons are displayed") {
                onTangemPayReceiveSheet {
                    copyButton.assertIsDisplayed()
                    shareButton.assertIsDisplayed()
                    showQrCodeButton.assertIsDisplayed()
                }
            }
            step("Click on 'Show QR' button") {
                onTangemPayReceiveSheet { showQrCodeButton.clickWithAssertion() }
            }
            step("Assert the QR code, the deposit address and its actions are displayed") {
                onTangemPayReceiveSheet {
                    qrCode.assertIsDisplayed()
                    address.assertTextEquals(bscDepositAddress)
                    copyButton.assertIsDisplayed()
                    shareButton.assertIsDisplayed()
                }
            }
            step("Assert the QR code encodes the deposit address") {
                assertTangemPayQrCodeEncodes(bscDepositAddress)
            }
        }
    }

    @AllureId("11380")
    @DisplayName("Tangem Pay: activating a not-issued network places one order and opens its receive sheet")
    @Test
    fun notIssuedNetworkActivationPlacesOrderTest() {
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val historyScenario = "tangem_pay_transaction_history"
        val historyEmptyState = "InitialEmpty"
        val activationScenario = "tangem_pay_network_activation"
        val activationPendingState = "NetworkActivationPending"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Set WireMock scenario '$historyScenario' to '$historyEmptyState'") {
                setWireMockScenarioState(historyScenario, historyEmptyState)
            }
            step("Set WireMock scenario '$activationScenario' to '$activationPendingState'") {
                setWireMockScenarioState(activationScenario, activationPendingState)
            }
            step("Open the 'Choose network' bottom sheet") { openTangemPayChooseNetworkSheetFromMain() }
            val lookupsBefore = smartContractOrderLookupCount()
            val ordersBefore = placedOrderCount()
            step("Click on the not-issued '$baseNetwork' network") {
                onTangemPayChooseNetworkSheet { fastOptionNetwork(baseNetwork).clickWithAssertion() }
            }
            step("Assert the '$baseNetwork' row progress subtitle is displayed") {
                onTangemPayChooseNetworkSheet {
                    flakySafely {
                        networkSubtitle(baseNetwork).assertTextContainsSafe(
                            getResourceString(CoreResR.string.tangempay_choose_network_row_loading),
                        )
                    }
                }
            }
            step("Assert the receive sheet for the issued '$baseNetwork' network is displayed") {
                assertTangemPayActivatedNetworkReceiveSheet(
                    networkName = baseNetwork,
                    tokensLabel = paymentTokensLabel,
                    depositAddress = baseDepositAddress,
                )
            }
            step("Assert the activation looked up existing orders and then placed exactly one") {
                assertEquals(
                    "Activation should look up existing orders once",
                    1,
                    smartContractOrderLookupCount() - lookupsBefore,
                )
                assertEquals("Activation should place exactly one order", 1, placedOrderCount() - ordersBefore)
            }
        }
    }

    @AllureId("11382")
    @DisplayName("Tangem Pay: an activation already in flight is resumed instead of being placed again")
    @Test
    fun notIssuedNetworkActivationResumesInFlightOrderTest() {
        val eligibilityPaeraCustomerState = "PaeraCustomer"
        val historyScenario = "tangem_pay_transaction_history"
        val historyEmptyState = "InitialEmpty"
        val activationScenario = "tangem_pay_network_activation"
        val activationInFlightState = "NetworkActivationInFlight"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityPaeraCustomerState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityPaeraCustomerState)
            }
            step("Set WireMock scenario '$historyScenario' to '$historyEmptyState'") {
                setWireMockScenarioState(historyScenario, historyEmptyState)
            }
            step("Set WireMock scenario '$activationScenario' to '$activationInFlightState'") {
                setWireMockScenarioState(activationScenario, activationInFlightState)
            }
            step("Open the 'Choose network' bottom sheet") { openTangemPayChooseNetworkSheetFromMain() }
            val lookupsBefore = smartContractOrderLookupCount()
            val ordersBefore = placedOrderCount()
            step("Click on the not-issued '$baseNetwork' network") {
                onTangemPayChooseNetworkSheet { fastOptionNetwork(baseNetwork).clickWithAssertion() }
            }
            step("Assert the receive sheet for the issued '$baseNetwork' network is displayed") {
                assertTangemPayActivatedNetworkReceiveSheet(
                    networkName = baseNetwork,
                    tokensLabel = paymentTokensLabel,
                    depositAddress = baseDepositAddress,
                )
            }
            step("Assert the in-flight order was resumed and no new one was placed") {
                assertEquals(
                    "A resumed activation should look up existing orders once",
                    1,
                    smartContractOrderLookupCount() - lookupsBefore,
                )
                assertEquals("A resumed activation should place no order", 0, placedOrderCount() - ordersBefore)
            }
        }
    }

    @AllureId("11383")
    @DisplayName("Tangem Pay: the deposit address of an enabled network is copied and shared")
    @Test
    fun enabledNetworkAddressCopyAndShareTest() {
        setupHooks().run {
            step("Set WireMock scenarios for a multichain Tangem Pay account") {
                setTangemPayMultiNetworkScenarios()
            }
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Stub the system share chooser") { stubShareChooserIntent() }
            step("Open the 'Add funds' bottom sheet") { openTangemPayAddFunds() }
            step("Open the receive sheet for the '$bscNetwork' network") {
                openTangemPayReceiveSheet(bscNetwork)
            }
            step("Click on 'Copy' button") {
                onTangemPayReceiveSheet { copyButton.clickWithAssertion() }
            }
            step("Assert the deposit address is put on the clipboard") {
                assertClipboardTextEquals(bscDepositAddress)
            }
            step("Click on 'Share' button") {
                onTangemPayReceiveSheet { shareButton.performClick() }
            }
            step("Assert the deposit address is passed to the system share chooser") {
                assertTextSharedViaChooser(bscDepositAddress, times = 1)
            }
            step("Click on 'Show QR' button") {
                onTangemPayReceiveSheet { showQrCodeButton.clickWithAssertion() }
            }
            step("Click on 'Copy' button on the QR code view") {
                onTangemPayReceiveSheet { copyButton.clickWithAssertion() }
            }
            step("Assert the deposit address is still the one on the clipboard") {
                assertClipboardTextEquals(bscDepositAddress)
            }
            step("Click on 'Share' button on the QR code view") {
                onTangemPayReceiveSheet { shareButton.performClick() }
            }
            step("Assert the deposit address is passed to the system share chooser again") {
                assertTextSharedViaChooser(bscDepositAddress, times = 2)
            }
        }
    }

    private fun smartContractOrderLookupCount(): Int = getWireMockRequestCountByQueryParam(
        method = "GET",
        urlPath = orderEndpoint,
        queryParam = orderTypesQueryParam,
        queryValue = smartContractOrderType,
    )

    private fun placedOrderCount(): Int = getWireMockRequestCount(method = "POST", urlPathPattern = orderEndpoint)
}