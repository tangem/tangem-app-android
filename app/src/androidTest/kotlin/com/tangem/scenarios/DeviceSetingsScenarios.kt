package com.tangem.scenarios

import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.screens.onDeviceSettingsScreen
import io.qameta.allure.kotlin.Allure.step

fun BaseTestCase.openResetCardScreen(withBackup: Boolean = false) {
    step("Click on 'Scan card or ring' button") {
        onDeviceSettingsScreen { scanCardOrRingButton.clickWithAssertion() }
    }
    step("Assert 'Reset to Factory Settings' button title is displayed") {
        onDeviceSettingsScreen { resetToFactorySettingsButtonTitle.assertIsDisplayed() }
    }
    step("Assert 'Reset to Factory Settings' button subtitle is displayed") {
        onDeviceSettingsScreen { resetToFactorySettingsButtonSubtitle(withBackup).assertIsDisplayed() }
    }
    step("Click on 'Reset to Factory Settings' button") {
        onDeviceSettingsScreen { resetToFactorySettingsButtonTitle.performClick() }
    }
}