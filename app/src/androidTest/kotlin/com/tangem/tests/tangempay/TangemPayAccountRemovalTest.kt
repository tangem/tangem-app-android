package com.tangem.tests.tangempay

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.*
import com.tangem.screens.tangempay.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayAccountRemovalTest : BaseTestCase() {

    private val eligibilityState = "PaeraCustomer"
    private val removalScenario = "tangem_pay_account_removal_android"
    private val deactivatedState = "Deactivated"

    private val packageName = getTargetContext().packageName

    @AllureId("10418")
    @DisplayName("Tangem Pay: a deactivated account is removed from the more-actions menu")
    @Test
    fun removeDeactivatedAccountFromMoreActionsTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(removalScenario, deactivatedState)
            },
        ).run {
            val removalRequestsBefore = getTangemPayRemovalRequestCount()

            step("Open the deactivated Tangem Pay payment account") { openDeactivatedTangemPay() }
            step("Click on 'More actions' button") {
                onTangemPayMainScreen { moreActionsButton.clickWithAssertion() }
            }
            step("Click on 'Remove account' menu item") {
                onTangemPayMainScreen { removeAccountMenuItem.clickWithAssertion() }
            }
            step("Confirm the account removal") { confirmTangemPayAccountRemoval() }
            step("Assert the account is removed") {
                assertTangemPayAccountRemoved(removalRequestsBefore, packageName)
            }
        }
    }

    @AllureId("10419")
    @DisplayName("Tangem Pay: a deactivated account is removed from the account-closed banner")
    @Test
    fun removeDeactivatedAccountFromBannerTest() {
        setupHooks(
            additionalBeforeSection = {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(removalScenario, deactivatedState)
            },
        ).run {
            val removalRequestsBefore = getTangemPayRemovalRequestCount()

            step("Open the deactivated Tangem Pay payment account") { openDeactivatedTangemPay() }
            step("Click on 'Remove account' button on the account-closed banner") {
                onTangemPayMainScreen { removeAccountBannerButton.clickWithAssertion() }
            }
            step("Confirm the account removal") { confirmTangemPayAccountRemoval() }
            step("Assert the account is removed") {
                assertTangemPayAccountRemoved(removalRequestsBefore, packageName)
            }
        }
    }
}