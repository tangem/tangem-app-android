package com.tangem.tests.moneyaccount

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.extensions.clickNearScreenBottom
import com.tangem.common.utils.assertUrlOpened
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.common.utils.stubUrlOpening
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.assertMoneyAccountMenuHasTermsAndSupportOnly
import com.tangem.scenarios.openMoneyAccount
import com.tangem.screens.moneyaccount.onMoneyAccountScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class MoneyAccountMenuTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED.rawName to true,
    )

    private val eligibilityState = "PaeraCustomer"
    private val moneyAccountScenario = "tangem_pay_money_account"
    private val termsTitle = "Terms and Limits"

    @AllureId("13340")
    @DisplayName("Money account menu: an account-only client sees 'Terms and Limits' and 'Support'")
    @Test
    fun accountOnlyClientMenuHasTermsAndSupportTest() {
        val accountOnlyState = "AccountActiveNoCards"
        val supportTitle = getResourceString(CoreResR.string.tangempay_pay_support)

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$accountOnlyState'") {
                setWireMockScenarioState(moneyAccountScenario, accountOnlyState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Click on 'Menu' button") { onMoneyAccountScreen { menuButton.performClick() } }
            step("Assert menu has only '$termsTitle' and 'Support'") {
                assertMoneyAccountMenuHasTermsAndSupportOnly(termsTitle)
            }
            step("Click outside the menu") { clickNearScreenBottom() }
            step("Assert menu is not displayed") {
                onMoneyAccountScreen {
                    flakySafely { menuItem(termsTitle).assertDoesNotExist() }
                    menuItem(supportTitle).assertDoesNotExist()
                }
            }
            step("Assert 'Money account' screen is displayed") {
                onMoneyAccountScreen { screenTitle.assertIsDisplayed() }
            }
        }
    }

    @AllureId("13341")
    @DisplayName("Money account menu: a card holder sees the same menu without 'Current plan'")
    @Test
    fun cardHolderMenuHasNoCurrentPlanTest() {
        val cardHolderState = "AccountActive"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$cardHolderState'") {
                setWireMockScenarioState(moneyAccountScenario, cardHolderState)
            }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Click on 'Menu' button") { onMoneyAccountScreen { menuButton.performClick() } }
            step("Assert menu has only '$termsTitle' and 'Support'") {
                assertMoneyAccountMenuHasTermsAndSupportOnly(termsTitle)
            }
        }
    }

    @AllureId("13342")
    @DisplayName("Money account menu: 'Terms and Limits' opens the account terms")
    @Test
    fun termsOpensAccountTermsTest() {
        val accountActiveState = "AccountActive"
        val accountTermsUrl = "https://tangem.com/docs/en/virtual-account-terms.pdf"

        setupHooks().run {
            step("Set WireMock scenario '$TANGEM_PAY_ELIGIBILITY_SCENARIO' to '$eligibilityState'") {
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
            }
            step("Set WireMock scenario '$moneyAccountScenario' to '$accountActiveState'") {
                setWireMockScenarioState(moneyAccountScenario, accountActiveState)
            }
            step("Stub opening '$accountTermsUrl'") { stubUrlOpening(accountTermsUrl) }
            step("Open 'Money account' screen") { openMoneyAccount() }
            step("Click on 'Menu' button") { onMoneyAccountScreen { menuButton.performClick() } }
            step("Click on '$termsTitle' menu item") {
                onMoneyAccountScreen { menuItem(termsTitle).performClick() }
            }
            step("Assert '$accountTermsUrl' is opened") {
                flakySafely(WAIT_UNTIL_TIMEOUT) { assertUrlOpened(accountTermsUrl) }
            }
            step("Assert menu is not displayed") {
                onMoneyAccountScreen { menuItem(termsTitle).assertDoesNotExist() }
            }
            step("Assert 'Money account' screen is displayed") {
                onMoneyAccountScreen { screenTitle.assertIsDisplayed() }
            }
        }
    }
}