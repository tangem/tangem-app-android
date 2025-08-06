package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.OpenMainScreenScenario
import com.tangem.screens.onDetailsScreen
import com.tangem.screens.onTopBar
import com.tangem.screens.onWalletSettingsScreen
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class DetailsTest : BaseTestCase() {

    @Test
    fun walletWithoutBackupDetailsTest() =
        setupHooks().run {
            scenario(OpenMainScreenScenario(composeTestRule))
            onTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is visible") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert 'Scan card' button is visible") {
                    scanCardButton.assertIsDisplayed()
                }
                step("Assert 'Buy Tangem card' button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms of service' button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Open 'Wallet settings' screen") {
                    walletNameButton.clickWithAssertion()
                }
            }
            onWalletSettingsScreen {
                step("Assert 'Link more cards' button is visible") {
                    linkMoreCardsButton.assertIsDisplayed()
                }
                step("Assert 'Card Settings' button is visible") {
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Referral program' button is visible") {
                    referralProgramButton.assertIsDisplayed()
                }
                step("Assert 'Forget wallet' button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }

    // @Test
    fun wallet2DetailsTest() =
        setupHooks().run {
            scenario(OpenMainScreenScenario(composeTestRule, ProductType.Wallet2))
            onTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button is visible") {
                    walletConnectButton.assertIsDisplayed()
                }
                step("Assert 'Scan card' button is visible") {
                    scanCardButton.assertIsDisplayed()
                }
                step("Assert 'Buy Tangem card' button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms or service' button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Open 'Wallet settings' screen") {
                    walletNameButton.clickWithAssertion()
                }
            }
            onWalletSettingsScreen {
                step("Assert 'Link more cards' button does not exist") {
                    linkMoreCardsButton.assertIsNotDisplayed()
                }
                step("Assert 'Card Settings' button is visible") {
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Referral program' button is visible") {
                    referralProgramButton.assertIsDisplayed()
                }
                step("Assert 'Forget wallet' button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }

    @Test
    fun noteDetailsTest() =
        setupHooks().run {
            scenario(OpenMainScreenScenario(composeTestRule, ProductType.Note))
            onTopBar {
                step("Open wallet details") {
                    moreButton.clickWithAssertion()
                }
            }
            onDetailsScreen {
                step("Assert 'Wallet connect' button does not exist") {
                    walletConnectButton.assertIsNotDisplayed()
                }
                step("Assert 'Scan card' button is visible") {
                    scanCardButton.assertIsDisplayed()
                }
                step("Assert 'Buy Tangem card' button is visible") {
                    buyTangemButton.assertIsDisplayed()
                }
                step("Assert 'App settings' button is visible") {
                    appSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Contact support' button is visible") {
                    contactSupportButton.assertIsDisplayed()
                }
                step("Assert 'Terms or service' button is visible") {
                    toSButton.assertIsDisplayed()
                }
                step("Open 'Wallet settings' screen") {
                    walletNameButton.clickWithAssertion()
                }
            }
            onWalletSettingsScreen {
                step("Assert 'Card Settings' button is visible") {
                    cardSettingsButton.assertIsDisplayed()
                }
                step("Assert 'Referral program' button does not exist") {
                    referralProgramButton.assertIsNotDisplayed()
                }
                step("Assert 'Forget wallet' button is visible") {
                    forgetWalletButton.assertIsDisplayed()
                }
            }
        }
}