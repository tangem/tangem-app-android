package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.scenarios.openMainScreen
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class WalletRenameTest : BaseTestCase() {

    @AllureId("2264")
    @DisplayName("Wallet details: rename wallet")
    @Test
    fun renameWalletTest() =
        setupHooks().run {
            val newWalletName = "Tangem QA"

            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Open wallet details") {
                onMainScreenTopBar { moreButton.clickWithAssertion() }
            }
            step("Open 'Wallet settings' screen") {
                onDetailsScreen { walletNameButton.clickWithAssertion() }
            }
            step("Click on 'Rename' button") {
                onWalletSettingsScreen { renameWalletButton.clickWithAssertion() }
            }
            step("Enter new wallet name '$newWalletName'") {
                onDialog { inputField.performTextReplacement(newWalletName) }
            }
            step("Click on 'OK' button") {
                onDialog { okButton.clickWithAssertion() }
            }
            step("Assert new wallet name '$newWalletName' is displayed on 'Wallet settings' screen") {
                onWalletSettingsScreen { walletNameValue(newWalletName).assertIsDisplayed() }
            }
            step("Click on 'Back' button") {
                onWalletSettingsScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Assert new wallet name '$newWalletName' is displayed on 'Details' screen") {
                onDetailsScreen { walletNameValue(newWalletName).assertIsDisplayed() }
            }
            step("Click on 'Back' button") {
                onDetailsScreen { topAppBarBackButton.clickWithAssertion() }
            }
            step("Assert new wallet name '$newWalletName' is displayed on 'Main' screen") {
                onMainScreen { walletNameText.assertTextContains(newWalletName) }
            }
        }
}