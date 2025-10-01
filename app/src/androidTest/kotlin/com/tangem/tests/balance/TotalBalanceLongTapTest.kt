package com.tangem.tests.balance

import androidx.compose.ui.test.longClick
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.TOTAL_BALANCE
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.synchronizeAddresses
import com.tangem.screens.onMainScreen
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class TotalBalanceLongTapTest : BaseTestCase() {

    @Test
    @AllureId("3965")
    @DisplayName("Total balance: check long tap on block without biometry")
    fun whenBiometryIsOffTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Synchronize addresses") {
                synchronizeAddresses(TOTAL_BALANCE)
            }
            step("Long tap on total balance block") {
                onMainScreen {
                    totalBalanceContainer.performTouchInput {
                        longClick()
                    }
                }
            }
            step("Assert 'Rename' button is not displayed") {
                onMainScreen { totalBalanceMenuRenameWallet.assertIsNotDisplayed() }
            }
            step("Assert 'Delete' button is not displayed") {
                onMainScreen { totalBalanceMenuDeleteWallet.assertIsNotDisplayed() }
            }
        }
    }
}