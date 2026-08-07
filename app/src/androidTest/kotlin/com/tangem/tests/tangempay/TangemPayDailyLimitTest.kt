package com.tangem.tests.tangempay

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TANGEM_PAY_ELIGIBILITY_SCENARIO
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.assertTextContainsSafe
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.res.R as CoreResR
import com.tangem.scenarios.openTangemPayDailyLimitSetup
import com.tangem.screens.onDialog
import com.tangem.screens.tangempay.onTangemPayCardPageScreen
import com.tangem.screens.tangempay.onTangemPayDailyLimitScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TangemPayDailyLimitTest : BaseTestCase() {

    private val dailyLimitScenario = "tangem_pay_daily_limit"
    private val highLimitState = "HighLimit"
    private val setErrorState = "SetError"
    private val eligibilityState = "PaeraCustomer"

    @AllureId("9726")
    @DisplayName("Tangem Pay: daily limit screen is displayed correctly")
    @Test
    fun dailyLimitScreenIsDisplayedCorrectlyTest() {
        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(dailyLimitScenario, highLimitState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(dailyLimitScenario)
            },
        ).run {
            step("Open daily limit setup screen") { openTangemPayDailyLimitSetup() }
            step("Assert amount field is displayed") {
                onTangemPayDailyLimitScreen { amountField.assertIsDisplayed() }
            }
            step("Assert hint is displayed") {
                onTangemPayDailyLimitScreen { hint.assertIsDisplayed() }
            }
            step("Assert 'Set limits' button is displayed") {
                onTangemPayDailyLimitScreen { setLimitsButton.assertIsDisplayed() }
            }
            step("Assert quick value preset '1' is displayed") {
                onTangemPayDailyLimitScreen { presetChip("1").assertIsDisplayed() }
            }
            step("Assert quick value preset '5000' is displayed") {
                onTangemPayDailyLimitScreen { presetChip("5000").assertIsDisplayed() }
            }
            step("Assert quick value preset '10000' is displayed") {
                onTangemPayDailyLimitScreen { presetChip("10000").assertIsDisplayed() }
            }
            step("Assert quick value preset '25000' is displayed") {
                onTangemPayDailyLimitScreen { presetChip("25000").assertIsDisplayed() }
            }
        }
    }

    @AllureId("9724")
    @DisplayName("Tangem Pay: selected quick value is applied to the amount field")
    @Test
    fun selectedQuickValueIsAppliedToAmountFieldTest() {
        val presetValue = "5000"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(dailyLimitScenario, highLimitState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(dailyLimitScenario)
            },
        ).run {
            step("Open daily limit setup screen") { openTangemPayDailyLimitSetup() }
            step("Click on '$presetValue' quick value preset") {
                onTangemPayDailyLimitScreen { presetChip(presetValue).clickWithAssertion() }
            }
            step("Assert amount field contains '$presetValue'") {
                onTangemPayDailyLimitScreen { amountField.assertTextContainsSafe(presetValue) }
            }
        }
    }

    @AllureId("9725")
    @DisplayName("Tangem Pay: 'Set limits' is disabled when the amount is above the limit")
    @Test
    fun setLimitsIsDisabledWhenAmountAboveLimitTest() {
        val amountAboveLimit = "300000"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(dailyLimitScenario, highLimitState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(dailyLimitScenario)
            },
        ).run {
            step("Open daily limit setup screen") { openTangemPayDailyLimitSetup() }
            step("Enter amount '$amountAboveLimit' above the limit") {
                onTangemPayDailyLimitScreen { amountField.performTextReplacement(amountAboveLimit) }
            }
            step("Assert 'Set limits' button is not enabled") {
                onTangemPayDailyLimitScreen { setLimitsButton.assertIsNotEnabled() }
            }
        }
    }

    @AllureId("9739")
    @DisplayName("Tangem Pay: successful daily limit setup")
    @Test
    fun successfulDailyLimitSetupTest() {
        val newLimit = "5000"
        val displayedLimit = "5,000"

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(dailyLimitScenario, highLimitState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(dailyLimitScenario)
            },
        ).run {
            step("Open daily limit setup screen") { openTangemPayDailyLimitSetup() }
            step("Enter amount '$newLimit'") {
                onTangemPayDailyLimitScreen { amountField.performTextReplacement(newLimit) }
            }
            step("Click on 'Set limits' button") {
                onTangemPayDailyLimitScreen { setLimitsButton.clickWithAssertion() }
            }
            step("Assert daily limit success screen is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayDailyLimitScreen { successTitle.assertIsDisplayed() }
                }
            }
            step("Click on 'Done' button") {
                onTangemPayDailyLimitScreen { doneButton.clickWithAssertion() }
            }
            step("Assert daily limit value contains '$displayedLimit'") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onTangemPayCardPageScreen {
                        dailyLimitValue.assertTextContainsSafe(displayedLimit, substring = true)
                    }
                }
            }
        }
    }

    @AllureId("9727")
    @DisplayName("Tangem Pay: error is shown when the limit change fails")
    @Test
    fun errorIsShownWhenLimitChangeFailsTest() {
        val newLimit = "5000"
        val errorTitle = getResourceString(CoreResR.string.common_something_went_wrong)

        setupHooks(
            additionalBeforeSection = {
                resetWireMockScenarios()
                setWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO, eligibilityState)
                setWireMockScenarioState(dailyLimitScenario, setErrorState)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(TANGEM_PAY_ELIGIBILITY_SCENARIO)
                resetWireMockScenarioState(dailyLimitScenario)
            },
        ).run {
            step("Open daily limit setup screen") { openTangemPayDailyLimitSetup() }
            step("Enter amount '$newLimit'") {
                onTangemPayDailyLimitScreen { amountField.performTextReplacement(newLimit) }
            }
            step("Click on 'Set limits' button") {
                onTangemPayDailyLimitScreen { setLimitsButton.clickWithAssertion() }
            }
            step("Assert limit change error dialog is displayed") {
                flakySafely(WAIT_UNTIL_TIMEOUT_LONG) {
                    onDialog {
                        title.assertTextContainsSafe(errorTitle)
                    }
                }
            }
            step("Click on 'OK' button") {
                onDialog { okButton.clickWithAssertion() }
            }
        }
    }
}