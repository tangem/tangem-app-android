package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.OpenMainScreenScenario
import com.tangem.screens.DetailsTestScreen
import com.tangem.screens.TestTopBar
import com.tangem.screens.WalletSettingsTestScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.kakaocup.compose.node.element.ComposeScreen
import org.junit.Test

@HiltAndroidTest
class DetailsScreenTest : BaseTestCase() {

    @Test
    fun walletWithoutBackupDetails() =
        setupHooks().run {
            scenario(OpenMainScreenScenario(composeTestRule))
            ComposeScreen.onComposeScreen<TestTopBar>(composeTestRule) {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<DetailsTestScreen>(composeTestRule) {
                step("Assert wallet connect button is visible") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert scan card button is visible") {
                    scanCardButton.assertIsDisplayed()
                }
                step("Assert buy Tangem card button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert app settings button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert contact support button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert terms or service button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Open wallet settings screen") {
                    walletNameButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<WalletSettingsTestScreen>(composeTestRule) {
                step("Assert Link more cards button is visible") {
                    linkMoreCardsButton.assertIsDisplayed()
                }
                step("Assert Card Settings button is visible") {
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert Referral program button is visible") {
                    referralProgramButton.assertIsDisplayed()
                }
                step("Assert Forget wallet button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }

    @Test
    fun wallet2Details() =
        setupHooks().run {
            scenario(OpenMainScreenScenario(composeTestRule, ProductType.Wallet2))
            ComposeScreen.onComposeScreen<TestTopBar>(composeTestRule) {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<DetailsTestScreen>(composeTestRule) {
                step("Assert wallet connect button is visible") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert scan card button is visible") {
                    scanCardButton.assertIsDisplayed()
                }
                step("Assert buy Tangem card button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert app settings button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert contact support button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert terms or service button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Open wallet settings screen") {
                    walletNameButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<WalletSettingsTestScreen>(composeTestRule) {
                step("Assert Link more cards button does not exist") {
                    linkMoreCardsButton.assertIsNotDisplayed()
                }
                step("Assert Card Settings button is visible") {
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert Referral program button is visible") {
                    referralProgramButton.assertIsDisplayed()
                }
                step("Assert Forget wallet button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }

    @Test
    fun noteDetails() =
        setupHooks().run {
            scenario(OpenMainScreenScenario(composeTestRule, ProductType.Note))
            ComposeScreen.onComposeScreen<TestTopBar>(composeTestRule) {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<DetailsTestScreen>(composeTestRule) {
                step("Assert wallet connect button does not exist") {
                    walletConnectButton.assertIsNotDisplayed()
                }
                step("Assert scan card button is visible") {
                    scanCardButton.assertIsDisplayed()
                }
                step("Assert buy Tangem card button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert app settings button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert contact support button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert terms or service button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Open wallet settings screen") {
                    walletNameButton.clickWithAssertion()
                }
            }
            ComposeScreen.onComposeScreen<WalletSettingsTestScreen>(composeTestRule) {
                step("Assert Card Settings button is visible") {
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert Referral program button does not exist") {
                    referralProgramButton.assertIsNotDisplayed()
                }
                step("Assert Forget wallet button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }
}