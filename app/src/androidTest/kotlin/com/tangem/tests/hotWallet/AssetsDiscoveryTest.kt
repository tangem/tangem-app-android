package com.tangem.tests.hotWallet

import androidx.test.InstrumentationRegistry.getTargetContext
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.CREATE_USER_WALLET_API_SCENARIO
import com.tangem.common.constants.TestConstants.MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO
import com.tangem.common.constants.TestConstants.PROVIDERS_API_SCENARIO
import com.tangem.common.constants.TestConstants.SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.SEED_PHRASE_HAPPY_PATH
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.WALLET_TOKENS_API_SCENARIO
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.restartApp
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.openMainScreenWithExistingHotWallet
import com.tangem.screens.*
import com.tangem.screens.accounts.onAccountDetailsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test
import com.tangem.core.ui.R as CoreUiR

@HiltAndroidTest
class AssetsDiscoveryTest : BaseTestCase() {

    private companion object {
        const val DISCOVERY_TIMEOUT_MILLIS = 120_000L

        const val SCENARIO_STATE_STARTED = "Started"
        const val SCENARIO_STATE_EMPTY = "Empty"
        const val SCENARIO_STATE_ALREADY_EXISTS = "AlreadyExists"
        const val SCENARIO_STATE_ASSETS_DISCOVERY_REDIRECT = "AssetsDiscoveryRedirect"
        const val SCENARIO_STATE_ASSETS_DISCOVERY_HAPPY_PATH = "AssetsDiscoveryHappyPath"
        const val SCENARIO_STATE_NON_ZERO_EVM_BALANCES = "NonZeroEvmBalances"
        const val SCENARIO_STATE_NON_ZERO_EVM_BALANCES_SLOW = "NonZeroEvmBalancesSlow"

        val EXPECTED_DISCOVERED_TOKENS = listOf(
            "Ethereum",
            "Polygon",
            "Tether",
        )

        val TOKENS_THAT_MUST_NOT_APPEAR = listOf(
            "Solana",
            "USDC",
        )

        val BACKEND_PRE_POPULATED_TOKENS = listOf(
            "Bitcoin",
            "Ethereum",
            "Polygon",
        )
    }

    @AllureId("9280")
    @DisplayName("Hot wallet: new import — Discovery → Sync → Banner → Check here happy path")
    @Test
    fun newHotWalletImportHappyPathTest() {
        val packageName = getTargetContext().packageName

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(PROVIDERS_API_SCENARIO, state = SCENARIO_STATE_ASSETS_DISCOVERY_REDIRECT)
                setWireMockScenarioState(CREATE_USER_WALLET_API_SCENARIO, state = SCENARIO_STATE_STARTED)
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, state = SCENARIO_STATE_ASSETS_DISCOVERY_HAPPY_PATH)
                setWireMockScenarioState(WALLET_TOKENS_API_SCENARIO, state = SCENARIO_STATE_STARTED)
                setWireMockScenarioState(MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO, state = SCENARIO_STATE_NON_ZERO_EVM_BALANCES)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(PROVIDERS_API_SCENARIO)
                resetWireMockScenarioState(CREATE_USER_WALLET_API_SCENARIO)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(WALLET_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO)
            },
        ).run {
            step("Import a new hot wallet from seed phrase") {
                openMainScreenWithExistingHotWallet(SEED_PHRASE_HAPPY_PATH)
            }
            step("Assert 'Restoring' progress loader is shown (discovery is in flight)") {
                onMainScreen { restoringProgressText.assertIsDisplayed() }
            }
            step("Wait for 'Wallet successfully imported' banner (discovery completes)") {
                flakySafely(timeoutMs = DISCOVERY_TIMEOUT_MILLIS) {
                    onMainScreen { walletImportedBanner.assertIsDisplayed() }
                }
            }
            step("Assert expected discovered tokens are visible in the assets list") {
                onMainScreen {
                    EXPECTED_DISCOVERED_TOKENS.forEach { token ->
                        tokenRowWithTitle(token).assertIsDisplayed()
                    }
                }
            }
            step("Tap 'Check here' (Manage tokens) on the banner") {
                onMainScreen { walletImportedBannerCheckHereButton.clickWithAssertion() }
            }
            step("Assert 'Manage Tokens' screen is opened") {
                onManageTokensScreen { searchField.assertIsDisplayed() }
            }
            step("Return to main screen") {
                device.uiDevice.pressBack()
                waitForIdle()
            }
            step("Assert banner is hidden after navigating into Manage Tokens") {
                onMainScreen { walletImportedBanner.assertIsNotDisplayed() }
            }
            step("Force-close and re-launch the app") {
                restartApp(packageName)
            }
            step("Assert banner is NOT shown again after relaunch") {
                onMainScreen { walletImportedBanner.assertIsNotDisplayed() }
            }
            step("Assert previously discovered tokens still appear in the assets list") {
                onMainScreen {
                    EXPECTED_DISCOVERED_TOKENS.forEach { token ->
                        tokenRowWithTitle(token).assertIsDisplayed()
                    }
                }
            }
            step("Assert zero-balance and spam tokens are NOT shown in the assets list") {
                onMainScreen {
                    TOKENS_THAT_MUST_NOT_APPEAR.forEach { token ->
                        assertTokenDoesNotExist(token)
                    }
                }
            }
        }
    }

    @AllureId("9284")
    @DisplayName("Hot wallet: token added manually during Discovery — no duplicate created")
    @Test
    fun manualTokenAddDuringDiscoveryNoDuplicateTest() {
        val tetherTitle = "Tether"
        val ethereumNetworkTitle = "ETHEREUM"
        val accountName = getResourceString(CoreUiR.string.account_main_account_title)
        val expectedTokensCount = 2

        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(PROVIDERS_API_SCENARIO, state = SCENARIO_STATE_ASSETS_DISCOVERY_REDIRECT)
                setWireMockScenarioState(CREATE_USER_WALLET_API_SCENARIO, state = SCENARIO_STATE_STARTED)
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, state = SCENARIO_STATE_ASSETS_DISCOVERY_HAPPY_PATH)
                setWireMockScenarioState(WALLET_TOKENS_API_SCENARIO, state = SCENARIO_STATE_STARTED)
                setWireMockScenarioState(
                    MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO,
                    state = SCENARIO_STATE_NON_ZERO_EVM_BALANCES_SLOW,
                )
            },
            additionalAfterSection = {
                resetWireMockScenarioState(PROVIDERS_API_SCENARIO)
                resetWireMockScenarioState(CREATE_USER_WALLET_API_SCENARIO)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(WALLET_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO)
            },
        ).run {
            step("Import a new hot wallet from seed phrase") {
                openMainScreenWithExistingHotWallet(SEED_PHRASE_HAPPY_PATH)
            }
            step("Assert 'Restoring' progress loader is shown (discovery is in flight)") {
                onMainScreen { restoringProgressText.assertIsDisplayed() }
            }
            step("Open wallet details from top bar") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings'") {
                onDetailsScreen { walletNameButton.performClick() }
            }
            step("Open account: '$accountName'") {
                onWalletSettingsScreen { accountItem(accountName).performClick() }
            }
            step("Open 'Manage Tokens' from account details") {
                onAccountDetailsScreen { manageTokensButton.performClick() }
            }
            step("Search for '$tetherTitle' in Manage Tokens") {
                onManageTokensScreen {
                    searchField.performClick()
                    searchField.performTextInput(tetherTitle)
                }
                device.uiDevice.pressBack()
                waitForIdle()
            }
            step("Expand '$tetherTitle'") {
                onManageTokensScreen { tokenItem(tetherTitle).clickWithAssertion() }
                waitForIdle()
            }
            step("Enable the $ethereumNetworkTitle network") {
                onManageTokensScreen { networkSwitch(ethereumNetworkTitle).clickWithAssertion() }
            }
            step("Save Manage Tokens changes") {
                onManageTokensScreen { saveButton.clickWithAssertion() }
                waitForIdle()
            }
            step("Navigate back to main screen") {
                repeat(times = 3) {
                    device.uiDevice.pressBack()
                    waitForIdle()
                }
            }
            step("Wait for 'Wallet successfully imported' banner (discovery completes after delay)") {
                flakySafely(timeoutMs = DISCOVERY_TIMEOUT_MILLIS) {
                    onMainScreen { walletImportedBanner.assertIsDisplayed() }
                }
            }
            step("Assert '$tetherTitle' is in the assets list (manual add + discovery merged)") {
                onMainScreen { tokenRowWithTitle(tetherTitle).assertIsDisplayed() }
            }
            step("Assert assets list contains exactly $expectedTokensCount tokens (no duplicate after merge)") {
                onMainScreen { assertTokensCount(expectedTokensCount) }
            }
        }
    }

    @AllureId("9282")
    @DisplayName("Hot wallet: re-import existing wallet — 200 OK, no Discovery, tokens from backend")
    @Test
    fun reimportExistingHotWalletTest() {
        setupHooks(
            additionalBeforeAppLaunchSection = {
                setWireMockScenarioState(CREATE_USER_WALLET_API_SCENARIO, state = SCENARIO_STATE_ALREADY_EXISTS)
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, state = SCENARIO_STATE_STARTED)
                setWireMockScenarioState(MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO, state = SCENARIO_STATE_EMPTY)
            },
            additionalAfterSection = {
                resetWireMockScenarioState(CREATE_USER_WALLET_API_SCENARIO)
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
                resetWireMockScenarioState(MORALIS_EVM_TOKEN_BALANCES_API_SCENARIO)
            },
        ).run {
            step("Import an existing hot wallet from seed phrase") {
                openMainScreenWithExistingHotWallet(SEED_PHRASE_12)
            }
            step("Assert tokens from backend are displayed immediately") {
                BACKEND_PRE_POPULATED_TOKENS.forEach { token ->
                    onMainScreen { tokenRowWithTitle(token).assertIsDisplayed() }
                }
            }
            step("Assert 'Restoring' loader is NOT displayed (discovery did not start)") {
                onMainScreen { restoringProgressText.assertIsNotDisplayed() }
            }
            step("Assert 'Wallet successfully imported' banner is NOT displayed") {
                onMainScreen { walletImportedBanner.assertIsNotDisplayed() }
            }
        }
    }
}