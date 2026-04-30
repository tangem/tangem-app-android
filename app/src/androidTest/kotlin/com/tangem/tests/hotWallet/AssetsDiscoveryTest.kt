package com.tangem.tests.hotWallet

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.CREATE_USER_WALLET_API_SCENARIO
import com.tangem.common.constants.TestConstants.SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_VERY_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.screens.onAssetsDiscoveryNotification
import com.tangem.screens.onManageTokensScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.Allure.step
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class AssetsDiscoveryTest : BaseTestCase() {

    private fun setupAssetsDiscoveryHooks() = setupHooks(
        additionalAfterSection = {
            resetWireMockScenarioState(CREATE_USER_WALLET_API_SCENARIO)
        },
    )

    private fun startDiscovery() {
        step("Set WireMock scenario '$CREATE_USER_WALLET_API_SCENARIO' to 'Started' (201)") {
            setWireMockScenarioState(
                scenarioName = CREATE_USER_WALLET_API_SCENARIO,
                state = "Started",
            )
        }
        step("Open 'Main Screen' with existing hot wallet") {
            openMainScreenWithExistingHotWallet(SEED_PHRASE_12)
        }
        step("Wait for 'Assets Discovery Completed' banner") {
            composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT_VERY_LONG) {
                runCatching {
                    onAssetsDiscoveryNotification { container.assertIsDisplayed() }
                }.isSuccess
            }
        }
    }

    @AllureId("9001")
    @DisplayName("Assets Discovery: completion banner appears after hot wallet import")
    @Test
    fun assetsDiscoveryCompletionBannerTest() {
        setupAssetsDiscoveryHooks().run {
            startDiscovery()

            step("Assert banner title and message are visible") {
                onAssetsDiscoveryNotification {
                    title.assertIsDisplayed()
                    message.assertIsDisplayed()
                }
            }
            step("Assert 'Manage tokens' and 'Close' buttons are visible") {
                onAssetsDiscoveryNotification {
                    manageTokensButton.assertIsDisplayed()
                    closeButton.assertIsDisplayed()
                }
            }
        }
    }

    @AllureId("9002")
    @DisplayName("Assets Discovery: dismiss completion banner with close button")
    @Test
    fun assetsDiscoveryDismissBannerTest() {
        setupAssetsDiscoveryHooks().run {
            startDiscovery()

            step("Click 'Close' button") {
                onAssetsDiscoveryNotification { closeButton.clickWithAssertion() }
            }
            step("Assert banner is no longer displayed") {
                composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT) {
                    !runCatching {
                        onAssetsDiscoveryNotification { container.assertIsDisplayed() }
                    }.isSuccess
                }
            }
        }
    }

    @AllureId("9003")
    @DisplayName("Assets Discovery: 'Manage tokens' button opens Manage Tokens screen")
    @Test
    fun assetsDiscoveryManageTokensClickTest() {
        setupAssetsDiscoveryHooks().run {
            startDiscovery()

            step("Click 'Manage tokens' button") {
                onAssetsDiscoveryNotification { manageTokensButton.clickWithAssertion() }
            }
            step("Assert 'Manage Tokens' screen is open") {
                composeTestRule.waitUntil(timeoutMillis = WAIT_UNTIL_TIMEOUT) {
                    runCatching {
                        onManageTokensScreen { searchField.assertIsDisplayed() }
                    }.isSuccess
                }
            }
        }
    }
}