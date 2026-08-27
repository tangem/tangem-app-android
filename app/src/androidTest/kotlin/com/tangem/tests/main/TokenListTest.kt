package com.tangem.tests.main

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.USER_TOKENS_API_SCENARIO
import com.tangem.common.constants.TestConstants.HOLD_DURATION_MS
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.resetWireMockScenarioState
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.scenarios.addNewCardWalletWithoutSync
import com.tangem.scenarios.getMainScreenTokensOrder
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.scenarios.waitUntilMainScreenTokenBalanceLoaded
import com.tangem.screens.onAddAndManageBottomSheet
import com.tangem.screens.DERIVED_TOKEN_ACTIONS
import com.tangem.screens.UNDERIVED_TOKEN_ACTIONS
import com.tangem.screens.onMainScreen
import com.tangem.screens.onTokenDetailsScreen
import com.tangem.tap.domain.sdk.mocks.content.Firmware412MockContent
import com.tangem.tap.domain.sdk.mocks.content.NodlMockContent
import com.tangem.tap.domain.sdk.mocks.content.NoteMockContent
import com.tangem.tap.domain.sdk.mocks.content.S2CMockContent
import com.tangem.tap.domain.sdk.mocks.content.TwinsMockContent
import com.tangem.tap.domain.sdk.mocks.content.V3MockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2MockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2PartialDerivationsMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet2WithDerivationsMockContent
import com.tangem.tap.domain.sdk.mocks.content.Wallet3MockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class TokenListTest : BaseTestCase() {

    @AllureId("168")
    @DisplayName("Token list: List displayed for Wallet 1.0 multicurrency card")
    @Test
    fun tokenListDisplayedForWallet1CardTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Add & manage' bottom sheet") {
                onMainScreen { addAndManageButton().assertIsDisplayed() }
                onMainScreen { addAndManageButton().clickWithAssertion() }
            }
            step("Assert 'Organize tokens' button is displayed") {
                onAddAndManageBottomSheet { organizeTokensButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("169")
    @DisplayName("Token list: Add&Manage unavailable for Note wallet")
    @Test
    fun tokenListAddAndManageUnavailableForNoteWalletTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = NoteMockContent)
            }
            step("Scroll to the token row (notification cards push the sole token below the fold)") {
                onMainScreen { scrollToTokenList() }
            }
            val tokensList = getMainScreenTokensOrder()

            step("Assert only one token in list is displayed") {
                assertEquals(
                    "Note wallet should display exactly one token, but was: $tokensList",
                    1,
                    tokensList.size,
                )
            }
            step("Assert 'Add & manage' button does not exist") {
                onMainScreen { addAndManageButtonNode.assertDoesNotExist() }
            }
        }
    }

    @AllureId("170")
    @DisplayName("Token list: List displayed for Nodl card")
    @Test
    fun tokenListDisplayedForNodlCardTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = NodlMockContent)
            }
            step("Assert 'Add & manage' button does not exist") {
                onMainScreen { addAndManageButtonNode.assertDoesNotExist() }
            }
        }
    }

    @AllureId("176")
    @DisplayName("Token list: Token context menu opening")
    @Test
    fun tokenListTokenContextMenuTest() {
        val bitcoinExchangeScenario = "express_api_assets"
        val bitcoinExchangeState = "BitcoinExchangeEnabled"
        val derivedTokenTitle = "Bitcoin"
        val missedDerivationTokenTitle = "Ethereum"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(bitcoinExchangeScenario)
            }
        ).run {
            step("Set WireMock scenario: '$bitcoinExchangeScenario' to state: '$bitcoinExchangeState'") {
                setWireMockScenarioState(bitcoinExchangeScenario, bitcoinExchangeState)
            }
            step("Open 'Main Screen' with partially-derived Wallet 2.0 card") {
                openMainScreen(mockContent = Wallet2PartialDerivationsMockContent)
            }
            step("Synchronize addresses without waiting for balances") {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Wait until '$derivedTokenTitle' balance is loaded") {
                waitUntilMainScreenTokenBalanceLoaded(derivedTokenTitle)
            }
            step("Assert 'Some addresses are missing' notification is displayed") {
                onMainScreen { missingAddressNotificationTitle.assertIsDisplayed() }
            }
            step("Long-tap on derived token in list") {
                onMainScreen { tokenWithTitleAndAddress(derivedTokenTitle).performTouchInput {
                    longClick(durationMillis = HOLD_DURATION_MS) } }
            }
            step("Assert context menu shows the expected actions for derived token '$derivedTokenTitle'") {
                onMainScreen { assertTokenContextMenuActions(DERIVED_TOKEN_ACTIONS, exact = false) }
            }
            step("Close context menu") {
                device.uiDevice.pressBack()
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Long-tap on not derived token in list") {
                onMainScreen { tokenRowWithTitle(missedDerivationTokenTitle).performTouchInput {
                    longClick(durationMillis = HOLD_DURATION_MS)
                } }
            }
            step("Assert context menu shows only 'Hide token' action for not derived token '$missedDerivationTokenTitle'") {
                onMainScreen { assertTokenContextMenuActions(UNDERIVED_TOKEN_ACTIONS) }
            }
        }
    }

    @AllureId("178")
    @DisplayName("Token list: Empty token list displaying")
    @Test
    fun tokenListEmptyTokenListDisplayingTest() {
        val userTokensScenario = USER_TOKENS_API_SCENARIO
        val userTokensState = "EmptyTokensList"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenario' to state: '$userTokensState'") {
                setWireMockScenarioState(userTokensScenario, userTokensState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Assert Token list is empty") {
                onMainScreen { assertTokensCount(0) }
            }
            step("Assert 'Markets' bottom sheet drag handle is displayed") {
                onMainScreen { marketsSheetDragHandle.assertIsDisplayed() }
            }
        }
    }

    @AllureId("180")
    @DisplayName("Token list: hide token by long tap")
    @Test
    fun checkCustomDerivationIconOnTokenAndNetworkTest() {
        val networkTitle = "Ethereum"
        val customTokenTitle = "Myria"
        val scenarioState = "CustomDerivation"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
            }
        ).run {

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$scenarioState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, scenarioState)
            }

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert token: '$networkTitle' is displayed") {
                onMainScreen { tokenWithTitleAndAddress(networkTitle).assertIsDisplayed() }
            }
            step("Assert token with custom derivation icon: '$customTokenTitle' is displayed") {
                onMainScreen { tokenWithCustomDerivationIcon(customTokenTitle).assertIsDisplayed() }
            }
        }
    }

    @AllureId("181")
    @DisplayName("Token list: Custom derivation icons are displaying")
    @Test
    fun tokenListCustomTokenIconsDisplaying() {
        val userTokensScenario = USER_TOKENS_API_SCENARIO
        val userTokensState = "CustomTokenAdded"
        val customTokenName = "Bitcoin"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set WireMock scenario: '$userTokensScenario' to state: '$userTokensState'") {
                setWireMockScenarioState(userTokensScenario, userTokensState)
            }
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Assert custom derivation icon is displayed on 'Main screen'") {
                onMainScreen { tokenWithCustomDerivationIcon(customTokenName).assertIsDisplayed() }
            }
            step("Click on '$customTokenName' token with custom derivation icon") {
                onMainScreen { tokenWithTitleAndAddress(customTokenName).clickWithAssertion() }
            }
            step("Assert custom derivation icon is displayed on 'Token details' screen") {
                onTokenDetailsScreen { customDerivationIcon.assertIsDisplayed() }
            }
        }
    }

    @AllureId("177")
    @DisplayName("Main: token list differs after switching to a second wallet")
    @Test
    fun tokenListChangedAfterSwitchingWalletTest() {
        val userTokensState = "ReducedTokens"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarioState(USER_TOKENS_API_SCENARIO)
            }
        ).run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }

            val firstWalletTokens = getMainScreenTokensOrder()

            step("Set WireMock scenario: '$USER_TOKENS_API_SCENARIO' to state: '$userTokensState'") {
                setWireMockScenarioState(USER_TOKENS_API_SCENARIO, userTokensState)
            }
            step("Add a second card wallet") {
                addNewCardWalletWithoutSync(Wallet2MockContent)
            }

            val secondWalletTokens = getMainScreenTokensOrder()

            step("Assert both wallets exposed a non-empty token list") {
                assertTrue(
                    "First wallet token list should not be empty",
                    firstWalletTokens.isNotEmpty(),
                )
                assertTrue(
                    "Second wallet token list should not be empty",
                    secondWalletTokens.isNotEmpty(),
                )
            }
            step("Assert the two wallets show different token lists") {
                assertNotEquals(
                    "Expected the second wallet's token list to differ from the first " +
                        "(first=$firstWalletTokens, second=$secondWalletTokens)",
                    firstWalletTokens,
                    secondWalletTokens,
                )
            }
        }
    }

    @AllureId("10341")
    @DisplayName("Token list: List displayed for Wallet 2.0 multicurrency card")
    @Test
    fun tokenListDisplayedForWalletV2CardTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = Wallet2WithDerivationsMockContent)
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Open 'Add & manage' bottom sheet") {
                onMainScreen { addAndManageButton().assertIsDisplayed() }
                onMainScreen { addAndManageButton().clickWithAssertion() }
            }
            step("Assert 'Organize tokens' button is displayed") {
                onAddAndManageBottomSheet { organizeTokensButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("10342")
    @DisplayName("Token list: List displayed for Wallet V3 multicurrency card")
    @Test
    fun tokenListDisplayedForWalletV3CardTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = V3MockContent)
            }
            step("Open 'Add & manage' bottom sheet") {
                onMainScreen { addAndManageButton().assertIsDisplayed() }
                onMainScreen { addAndManageButton().clickWithAssertion() }
            }
            step("Assert 'Organize tokens' button is displayed") {
                onAddAndManageBottomSheet { organizeTokensButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("10837")
    @DisplayName("Token list: List displayed for Wallet 3.0 multicurrency card")
    @Test
    fun tokenListDisplayedForWallet3CardTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = Wallet3MockContent)
            }
            step("Open 'Add & manage' bottom sheet") {
                onMainScreen { addAndManageButton().assertIsDisplayed() }
                onMainScreen { addAndManageButton().clickWithAssertion() }
            }
            step("Assert 'Organize tokens' button is displayed") {
                onAddAndManageBottomSheet { organizeTokensButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("10344")
    @DisplayName("Token list: List displayed for Wallet 1.0 (4.12) multicurrency card")
    @Test
    fun tokenListDisplayedFor412CardTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = Firmware412MockContent)
            }
            step("Open 'Add & manage' bottom sheet") {
                onMainScreen { addAndManageButton().assertIsDisplayed() }
                onMainScreen { addAndManageButton().clickWithAssertion() }
            }
            step("Assert 'Organize tokens' button is displayed") {
                onAddAndManageBottomSheet { organizeTokensButton.assertIsDisplayed() }
            }
        }
    }

    @AllureId("10345")
    @DisplayName("Token list: Add&Manage unavailable for Twin wallet")
    @Test
    fun tokenListAddAndManageUnavailableForTwinWalletTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = TwinsMockContent, isTwinsCard = true)
            }
            val tokensList = getMainScreenTokensOrder()

            step("Assert only one token in list is displayed") {
                assertEquals(
                    "Twin wallet should display exactly one token, but was: $tokensList",
                    1,
                    tokensList.size,
                )
            }
            step("Assert 'Add & manage' button does not exist") {
                onMainScreen { addAndManageButtonNode.assertDoesNotExist() }
            }
        }
    }

    @AllureId("10346")
    @DisplayName("Token list: Add&Manage unavailable for S2C wallet")
    @Test
    fun tokenListAddAndManageUnavailableForS2CWalletTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = S2CMockContent)
            }
            val tokensList = getMainScreenTokensOrder()

            step("Assert only one token in list is displayed") {
                assertEquals(
                    "S2C wallet should display exactly one token, but was: $tokensList",
                    1,
                    tokensList.size,
                )
            }
            step("Assert 'Add & manage' button does not exist") {
                onMainScreen { addAndManageButtonNode.assertDoesNotExist() }
            }
        }
    }
}