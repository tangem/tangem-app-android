package com.tangem.tests

import com.atiurin.ultron.allure.step.step
import com.atiurin.ultron.extensions.assertIsDisplayed
import com.atiurin.ultron.extensions.assertIsNotDisplayed
import com.atiurin.ultron.extensions.click
import com.tangem.common.BaseTestCase
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.MainPageScenario
import com.tangem.screens.DetailsPage
import com.tangem.screens.TopBarPage
import com.tangem.screens.WalletSettingsPage
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class DetailsScreenTest : BaseTestCase() {

    @Test
    fun walletWithoutBackupDetails() {
        MainPageScenario.open()
        step("Open wallet details") {
            TopBarPage.moreButton.click()
        }
        step("Assert wallet connect button is visible") {
            DetailsPage.walletConnectButton.assertIsDisplayed()
        }
        step("Assert scan card button is visible") {
            DetailsPage.scanCardButton.assertIsDisplayed()
        }
        step("Assert buy Tangem card button is visible") {
            DetailsPage.buyTangemButton.assertIsDisplayed()
        }
        step("Assert app settings button is visible") {
            DetailsPage.appSettingsButton.assertIsDisplayed()
        }
        step("Assert contact support button is visible") {
            DetailsPage.contactSupportButton.assertIsDisplayed()
        }
        step("Assert terms or service button is visible") {
            DetailsPage.toSButton.assertIsDisplayed()
        }
        step("Open wallet settings screen") {
            DetailsPage.walletNameButton.click()
        }
        step("Assert Link more cards button is visible") {
            WalletSettingsPage.linkMoreCardsButton.assertIsDisplayed()
        }
        step("Assert Card Settings button is visible") {
            WalletSettingsPage.cardSettingsButton.assertIsDisplayed()
        }
        step("Assert Referral program button is visible") {
            WalletSettingsPage.referralProgramButton.assertIsDisplayed()
        }
        step("Assert Forget wallet button is visible") {
            WalletSettingsPage.forgetWalletButton.assertIsDisplayed()
        }
    }

    @Test
    fun wallet2Details() {
        MainPageScenario.open(ProductType.Wallet2)
        step("Open wallet details") {
            TopBarPage.moreButton.click()
        }
        step("Assert wallet connect button is visible") {
            DetailsPage.walletConnectButton.assertIsDisplayed()
        }
        step("Assert scan card button is visible") {
            DetailsPage.scanCardButton.assertIsDisplayed()
        }
        step("Assert buy Tangem card button is visible") {
            DetailsPage.buyTangemButton.assertIsDisplayed()
        }
        step("Assert app settings button is visible") {
            DetailsPage.appSettingsButton.assertIsDisplayed()
        }
        step("Assert contact support button is visible") {
            DetailsPage.contactSupportButton.assertIsDisplayed()
        }
        step("Assert terms or service button is visible") {
            DetailsPage.toSButton.assertIsDisplayed()
        }
        step("Open wallet settings screen") {
            DetailsPage.walletNameButton.click()
        }
        step("Assert Link more cards button does not exist") {
            WalletSettingsPage.linkMoreCardsButton.assertIsNotDisplayed()
        }
        step("Assert Card Settings button is visible") {
            WalletSettingsPage.cardSettingsButton.assertIsDisplayed()
        }
        step("Assert Referral program button is visible") {
            WalletSettingsPage.referralProgramButton.assertIsDisplayed()
        }
        step("Assert Forget wallet button is visible") {
            WalletSettingsPage.forgetWalletButton.assertIsDisplayed()
        }
    }

    @Test
    fun noteDetails() {
        MainPageScenario.open(ProductType.Note)
        step("Open wallet details") {
            TopBarPage.moreButton.click()
        }
        step("Assert wallet connect button does not exist") {
            DetailsPage.walletConnectButton.assertIsNotDisplayed()
        }
        step("Assert scan card button is visible") {
            DetailsPage.scanCardButton.assertIsDisplayed()
        }
        step("Assert buy Tangem card button is visible") {
            DetailsPage.buyTangemButton.assertIsDisplayed()
        }
        step("Assert app settings button is visible") {
            DetailsPage.appSettingsButton.assertIsDisplayed()
        }
        step("Assert contact support button is visible") {
            DetailsPage.contactSupportButton.assertIsDisplayed()
        }
        step("Assert terms or service button is visible") {
            DetailsPage.toSButton.assertIsDisplayed()
        }
        step("Open wallet settings screen") {
            DetailsPage.walletNameButton.click()
        }
        step("Assert Card Settings button is visible") {
            WalletSettingsPage.cardSettingsButton.assertIsDisplayed()
        }
        step("Assert Referral program button does not exist") {
            WalletSettingsPage.referralProgramButton.assertIsNotDisplayed()
        }
        step("Assert Forget wallet button is visible") {
            WalletSettingsPage.forgetWalletButton.assertIsDisplayed()
        }
    }
}
