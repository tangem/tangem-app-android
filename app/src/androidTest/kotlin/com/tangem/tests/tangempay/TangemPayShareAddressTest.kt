package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.assertTextSharedViaChooser
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubShareChooserIntent
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayShareAddressTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val channelsVirtualAccountState = "VirtualAccount"

    // The account has to carry a network for 'Add funds' to be enabled at all; this is the only Android
    // customer-state mock that does, and it brings the bank-transfer option the manual case expects too.
    private val accountScenario = "tangem_pay_virtual_account"
    private val accountActiveState = "AccountActive"

    private val depositNetwork = "Polygon"

    @AllureId("9551")
    @DisplayName("Tangem Pay: the deposit address is shared from the Add funds sheet and from its QR code")
    @Test
    fun shareTangemPayDepositAddressTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_CHANNELS_SCENARIO, channelsVirtualAccountState)
                setWireMockScenarioState(accountScenario, accountActiveState)
            },
        ).run {
            step("Open Tangem Pay payment account") { openTangemPay() }
            step("Stub the system share chooser") { stubShareChooserIntent() }
            step("Open the 'Add funds' bottom sheet") { openTangemPayAddFunds() }
            step("Assert the 'Add funds' sheet offers all three top-up options") {
                onTangemPayAddFundsSheet {
                    bankTransferOption.assertIsDisplayed()
                    swapOption.assertIsDisplayed()
                    receiveOption.assertIsDisplayed()
                }
            }
            var depositAddress = ""
            step("Open the receive sheet for the '$depositNetwork' network") {
                depositAddress = openTangemPayReceiveSheet(depositNetwork)
            }
            step("Assert 'Copy' and 'Share' buttons are displayed") {
                onTangemPayReceiveSheet {
                    copyButton.assertIsDisplayed()
                    shareButton.assertIsDisplayed()
                }
            }
            step("Click on 'Share' button") {
                onTangemPayReceiveSheet { shareButton.performClick() }
            }
            step("Assert the deposit address is passed to the system share chooser") {
                assertTextSharedViaChooser(depositAddress, times = 1)
            }
            step("Click on 'Show QR code' button") {
                onTangemPayReceiveSheet { showQrCodeButton.clickWithAssertion() }
            }
            step("Assert the QR code and the deposit address are displayed") {
                onTangemPayReceiveSheet {
                    qrCode.assertIsDisplayed()
                    address.assertTextEquals(depositAddress)
                    copyButton.assertIsDisplayed()
                    shareButton.assertIsDisplayed()
                }
            }
            step("Assert the QR code encodes the deposit address") {
                assertTangemPayQrCodeEncodes(depositAddress)
            }
            step("Click on 'Share' button on the QR code view") {
                onTangemPayReceiveSheet { shareButton.performClick() }
            }
            step("Assert the deposit address is passed to the system share chooser again") {
                assertTextSharedViaChooser(depositAddress, times = 2)
            }
        }
    }
}