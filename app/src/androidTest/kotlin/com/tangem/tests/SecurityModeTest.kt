package com.tangem.tests

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.domain.models.scan.ProductType
import com.tangem.scenarios.openDeviceSettingsScreen
import com.tangem.scenarios.openMainScreen
import com.tangem.screens.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.qameta.allure.kotlin.AllureId
import io.qameta.allure.kotlin.junit4.DisplayName
import org.junit.Test

@HiltAndroidTest
class SecurityModeTest : BaseTestCase() {

    @AllureId("2267")
    @DisplayName("Security Mode: Twin card opens the section")
    @Test
    fun twinSecurityModeOpensTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen(productType = ProductType.Twins, isTwinsCard = true)
            }
            openDeviceSettingsScreen()
            step("Click on 'Scan card or ring' button") {
                onDeviceSettingsScreen { scanCardOrRingButton.clickWithAssertion() }
            }
            step("Assert 'Security Mode' row is enabled") {
                onDeviceSettingsScreen { securityModeRow.assertIsEnabled() }
            }
            step("Click on 'Security Mode' row") {
                onDeviceSettingsScreen { securityModeRow.clickWithAssertion() }
            }
            step("Assert 'Security Mode' screen is displayed") {
                onSecurityModeScreen { screenContainer.assertIsDisplayed() }
            }
        }

    @DisplayName("Security Mode: other cards cannot open the section")
    @Test
    fun walletSecurityModeDisabledTest() =
        setupHooks().run {
            step("Open 'Main Screen'") {
                openMainScreen()
            }
            openDeviceSettingsScreen()
            step("Click on 'Scan card or ring' button") {
                onDeviceSettingsScreen { scanCardOrRingButton.clickWithAssertion() }
            }
            step("Assert 'Security Mode' row is not clickable") {
                onDeviceSettingsScreen { securityModeRow.assertIsNotEnabled() }
            }
        }
}