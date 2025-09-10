package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.*
import com.tangem.tap.domain.sdk.mocks.content.BackupWalletMockContent
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Ignore
import org.junit.Test

@HiltAndroidTest
class ResetCardTest : BaseTestCase() {

    @AllureId("3988")
    @DisplayName("Reset card: reset Wallet 1.0 card without backup")
    @Test
    fun resetWallet1CardWithoutBackupTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Open 'Device settings' screen") {
                openDeviceSettingsScreen()
            }
            step("Open 'Reset card' screen") {
                openResetCardScreen()
            }
            step("Check 'Reset card' screen") {
                checkResetCardScreen()
            }
            step("Check checkbox logic") {
                checkCheckBoxLogic()
            }
        }
    }

    @AllureId("3987")
    @DisplayName("Reset card: reset Wallet 1.0 card with backup")
    @Test
    fun resetWallet1CardWithBackupTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(mockContent = BackupWalletMockContent)
            }
            step("Open 'Device settings' screen") {
                openDeviceSettingsScreen()
            }
            step("Open 'Reset card' screen") {
                openResetCardScreen(withBackup = true)
            }
            step("Check 'Reset card' screen") {
                checkResetCardScreen(withBackup = true)
            }
            step("Check checkbox logic") {
                checkCheckBoxLogic(withBackup = true)
            }
        }
    }

    @AllureId("3974")
    @DisplayName("Reset card: reset Wallet 2.0 card with backup")
    @Ignore("toDo [REDACTED_TASK_KEY]: On CI Already used wallet dialog doesn't displayed")
    @Test
    fun resetWallet2CardWithBackupTest() {
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(productType = ProductType.Wallet2, alreadyActivatedDialogIsShown = true)
            }
            step("Open 'Device settings' screen") {
                openDeviceSettingsScreen()
            }
            step("Open 'Reset card' screen") {
                openResetCardScreen(withBackup = true)
            }
            step("Check 'Reset card' screen") {
                checkResetCardScreen(withBackup = true)
            }
            step("Check checkbox logic") {
                checkCheckBoxLogic(withBackup = true)
            }
        }
    }
}