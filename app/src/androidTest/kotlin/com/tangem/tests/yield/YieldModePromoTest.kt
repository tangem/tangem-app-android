package com.tangem.tests.yield

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.screens.onYieldModePromoBottomSheet
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.core.res.R as CoreResR
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class YieldModePromoTest : BaseTestCase() {

    override val featureToggleOverrides = mapOf(
        FeatureToggles.TWI_1762_YIELD_MODE_PROMO_ENABLED.rawName to true,
    )

    @AllureId("12893")
    @DisplayName("Yield mode promo: the main screen offers the best APY among the wallet's tokens")
    @Test
    fun promoShownOnMainScreenTest() {
        val expectedTitle = getResourceString(CoreResR.string.yield_module_main_promo_title, "5.24")

        setupHooks(
            additionalBeforeAppLaunchSection = { System.setProperty(EXPERIMENT_PROPERTY, USER_MAX_APY_VARIANT) },
            additionalPreferencesSection = { preferences ->
                // Any touch on the main screen ends the check, so the tooltip that asks for one stays hidden
                preferences[StorePreferencesKeys().shouldShowMarketsTooltipKey] = false
            },
            additionalAfterSection = { System.clearProperty(EXPERIMENT_PROPERTY) },
        ).run {
            step("Set WireMock scenario: '$PORTFOLIO_SCENARIO' to state: '$PORTFOLIO_STATE'") {
                setWireMockScenarioState(scenarioName = PORTFOLIO_SCENARIO, state = PORTFOLIO_STATE)
            }
            step("Set WireMock scenario: '$BALANCES_SCENARIO' to state: '$BALANCES_STATE'") {
                setWireMockScenarioState(scenarioName = BALANCES_SCENARIO, state = BALANCES_STATE)
            }
            step("Set WireMock scenario: '$YIELD_SCENARIO' to state: '$YIELD_NOT_ACTIVE_STATE'") {
                setWireMockScenarioState(scenarioName = YIELD_SCENARIO, state = YIELD_NOT_ACTIVE_STATE)
            }
            step("Open 'Main Screen' with existing hot wallet") {
                openMainScreenWithExistingHotWallet(seedPhrase = SVS_SEED_PHRASE_12, dismissMarketsTooltip = false)
            }
            step("Assert the promo bottom sheet offers '$expectedTitle'") {
                onYieldModePromoBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT_LONG) { title.assertTextEquals(expectedTitle) }
                }
            }
            step("Click on 'Close' button") {
                onYieldModePromoBottomSheet { closeButton.clickWithAssertion() }
            }
            step("Assert the promo bottom sheet is closed") {
                onYieldModePromoBottomSheet {
                    flakySafely(WAIT_UNTIL_TIMEOUT) { tryButton.assertDoesNotExist() }
                }
            }
        }
    }

    private companion object {
        const val EXPERIMENT_PROPERTY = "uitest.abtests.twi_1762_yield_mode_promo"
        const val USER_MAX_APY_VARIANT = "user_max_apy"
        const val PORTFOLIO_SCENARIO = "user_tokens_api"
        const val PORTFOLIO_STATE = "YieldUSDCEthereum"
        const val BALANCES_SCENARIO = "moralis_evm_token_balances_api"
        const val BALANCES_STATE = "NonZeroEvmBalances"
        const val YIELD_SCENARIO = "yield_supply_status"
        const val YIELD_NOT_ACTIVE_STATE = "NotActive"
    }
}