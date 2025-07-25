package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.scenarios.OpenMainScreenScenario
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class HideTokenTest : BaseTestCase() {

    @AllureId("880")
    @DisplayName("Hide token in 'Token details' screen by 'Hide' button in topBar menu")
    @Test
    fun hideWalletTokenByHideButtonTest() {
        val tokenTitle = "Polygon"
        val balance = "<$0.01"
        setupHooks().run {
            step("Open 'Main Screen'") {
                scenario(OpenMainScreenScenario(composeTestRule))
            }
            step("Click on 'Synchronize addresses' button" ) {
                onMainScreen { synchronizeAddressesButton.clickWithAssertion() }
            }
            step("Assert wallet balance = $balance") {
                onMainScreen { walletBalance().assertTextContains(balance) }
            }
            step("Click on token with name: '$tokenTitle'") {
                onMainScreen { tokenWithTitleAndAddress(tokenTitle).clickWithAssertion() }
            }
            step("Assert 'Token details screen' open") {
                onTokenDetailsScreen { screenContainer.assertIsDisplayed() }
            }
            step("Click 'More button'") {
                onTokenDetailsTopBar { moreButton.clickWithAssertion() }
            }
            step("Click 'Hide token' button") {
                onPopUpMenu {
                    popUpContainer.assertIsDisplayed()
                    hideTokenButton.clickWithAssertion()
                }
            }
            step("Click 'Hide' button in dialog") {
                onDialog {
                    dialogContainer.assertIsDisplayed()
                    hideButton.clickWithAssertion()
                }
            }
            step("Assert token: '$tokenTitle' is not displayed") {
                onMainScreen { assertTokenDoesNotExist(tokenTitle) }
            }
        }
    }

}