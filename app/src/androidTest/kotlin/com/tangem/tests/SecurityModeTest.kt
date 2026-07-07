package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openDeviceSettingsScreen
import com.tangem.scenarios.openMainScreen
import com.tangem.scenarios.scanCardInDeviceSettings
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SecurityModeTest : BaseTestCase() {

    @AllureId("2267")
    @DisplayName("Security Mode: available for Twin cards")
    @Test
    fun securityModeOpensForTwinsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(productType = ProductType.Twins, isTwinsCard = true)
            }
            step("Open 'Device settings' screen") {
                openDeviceSettingsScreen()
            }
            step("Scan card in 'Device settings'") {
                scanCardInDeviceSettings()
            }
            step("Assert 'Security mode' row is enabled") {
                onDeviceSettingsScreen { securityModeRow.assertIsEnabled() }
            }
            step("Click on 'Security mode' button") {
                onDeviceSettingsScreen { securityModeRow.performClick() }
            }
            onSecurityModeScreen {
                step("Assert 'Long tap' option is displayed") {
                    longTapOptionDescription.assertIsDisplayed()
                }
                step("Assert 'Save changes' button is displayed") {
                    saveChangesButton.assertIsDisplayed()
                }
            }
        }

    @AllureId("9831")
    @DisplayName("Security Mode: unavailable for single-capability cards")
    @Test
    fun securityModeRowDisabledForOtherCardsTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            step("Open 'Device settings' screen") {
                openDeviceSettingsScreen()
            }
            step("Scan card in 'Device settings'") {
                scanCardInDeviceSettings()
            }
            step("Assert 'Security mode' row title is displayed") {
                onDeviceSettingsScreen { securityModeRowTitle.assertIsDisplayed() }
            }
            step("Assert 'Security mode' row is disabled") {
                onDeviceSettingsScreen { securityModeRow.assertIsNotEnabled() }
            }
        }
}