package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.*
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
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
        val balance = TOTAL_BALANCE
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(balance)
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