package com.tangem.core.navigation.settings

class DummySettingsManager : SettingsManager {
    override fun openAppSettings() = Unit
    override fun openAppNotificationSettings() = Unit
    override fun openBiometricSettings() = Unit
}