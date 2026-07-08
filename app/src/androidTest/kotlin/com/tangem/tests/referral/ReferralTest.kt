package com.tangem.tests.referral

import androidx.test.core.app.ApplicationProvider
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.extensions.tapBackButton
import com.tangem.common.utils.AddressComparisonHelper
import com.tangem.common.utils.getClipboardText
import com.tangem.common.utils.resetWireMockScenarios
import com.tangem.common.utils.setWireMockScenarioState
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.openTesterMenu
import com.tangem.scenarios.referralTakeParticipate
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onMainScreen
import com.tangem.screens.onMainScreenTopBar
import com.tangem.screens.onTesterMenuScreen
import com.tangem.screens.onWalletSettingsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Test

@HiltAndroidTest
class ReferralTest : BaseTestCase() {

    @AllureId("3630")
    @DisplayName("Referral program: Token and Blockchain added on Main screen after participation")
    @Test
    fun referralTokenAndBlockchainAddedAfterParticipationTest() {
        val tokenNetwork = "Tron"
        val token = "Tether"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Verify network $tokenNetwork and token $token is not displayed") {
                onMainScreen {
                    assertTokenDoesNotExist(tokenNetwork)
                    assertTokenDoesNotExist(token)
                }
            }
            step("Take participate in Referral program") {
                referralTakeParticipate()
            }
            step("Return to the 'Main' screen") {
                tapBackButton()
                onWalletSettingsScreen { screenContainer.assertIsDisplayed() }
                tapBackButton()
                onDetailsScreen { screenContainer.assertIsDisplayed() }
                tapBackButton()
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Verify network $tokenNetwork and token $token is displayed on 'Main' screen") {
                onMainScreen {
                    tokenWithTitleAndAddress(tokenNetwork)
                    tokenWithTitleAndAddress(token)
                }
            }
        }
    }

    @AllureId("10098")
    @DisplayName("Referral program: Token added on Main screen after participation")
    @Test
    fun referralTokenAddedAfterParticipationTest() {
        val userWalletScenarioName = "user_tokens_api"
        val userWalletState = "Tron"
        val tokenNetwork = "Tron"
        val token = "Tether"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Set wiremock scenario: $userWalletScenarioName to state $userWalletState") {
                setWireMockScenarioState(scenarioName = userWalletScenarioName, state = userWalletState)
            }
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Verify token $token is not displayed") {
                onMainScreen { assertTokenDoesNotExist(token) }
            }
            step("Verify network $tokenNetwork is displayed") {
                onMainScreen { tokenWithTitleAndAddress(tokenNetwork) }
            }
            step("Take participate in Referral program") {
                referralTakeParticipate()
            }
            step("Return to the 'Main' screen") {
                tapBackButton()
                onWalletSettingsScreen { screenContainer.assertIsDisplayed() }
                tapBackButton()
                onDetailsScreen { screenContainer.assertIsDisplayed() }
                tapBackButton()
                onMainScreen { screenContainer.assertIsDisplayed() }
            }
            step("Verify $token is displayed on 'Main' screen") {
                onMainScreen { tokenWithTitleAndAddress(token) }
            }
        }
    }

    @AllureId("3629")
    @DisplayName("Referral program: Participating unavailable for No-Wallet cards")
    @Test
    fun referralUnavailableForNoWalletCardsTest() {
        setupHooks().run {
            step("Open 'Main' screen") {
                openMainScreen(productType = ProductType.Note)
            }
            step("Open 'Details' screen") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }
            step("Verify 'Referral program' button is not displayed") {
                onWalletSettingsScreen { referralProgramButton.assertDoesNotExist() }
            }
        }
    }

    @AllureId("3636")
    @DisplayName("Referral program: Verify wallet derivation")
    @Test
    fun referralVerifyWalletDerivationTest() {
        val tokenNetwork = "Tron"
        val expectedDerivationPath = "m/44'/195'/0'/0/0"

        setupHooks(
            additionalAfterSection = {
                resetWireMockScenarios()
            }
        ).run {
            step("Open 'Main' screen") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses()
            }
            step("Take participate in Referral program") {
                referralTakeParticipate()
            }
            step("Open Debug menu") {
                openTesterMenu()
            }
            step("Open 'Addresses info' screen") {
                onTesterMenuScreen { addressesInfoButton.clickWithAssertion() }
            }
            step("Verify $tokenNetwork derivation path is '$expectedDerivationPath'") {
                onTesterMenuScreen { jsonTab.clickWithAssertion() }
                onTesterMenuScreen { copyButton.clickWithAssertion() }
                val addressesJson = getClipboardText(ApplicationProvider.getApplicationContext())
                    ?: error("Clipboard is empty after copying addresses")
                val actualDerivationPath =
                    AddressComparisonHelper.derivationPathForBlockchain(addressesJson, tokenNetwork)
                assertEquals(expectedDerivationPath, actualDerivationPath)
            }
        }
    }
}