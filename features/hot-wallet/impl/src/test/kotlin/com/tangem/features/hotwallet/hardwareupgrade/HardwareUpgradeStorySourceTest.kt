package com.tangem.features.hotwallet.hardwareupgrade

import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class HardwareUpgradeStorySourceTest {

    @ParameterizedTest
    @EnumSource(AppRoute.WalletHardwareBackup.Source::class)
    fun `GIVEN an upgrade entry point WHEN mapped to the story source THEN the story reports the expected source`(
        source: AppRoute.WalletHardwareBackup.Source,
    ) {
        // Arrange
        val expectedAnalyticsValue = when (source) {
            AppRoute.WalletHardwareBackup.Source.UPGRADE_BANNER -> "Main"
            AppRoute.WalletHardwareBackup.Source.PERMANENT_BANNER -> "Permanent Banner"
            AppRoute.WalletHardwareBackup.Source.WALLET_SETTINGS -> "Wallet Settings"
            AppRoute.WalletHardwareBackup.Source.BACKUP -> "Main"
        }

        // Act
        val actual = source.toStoryV2Source().analyticsValue

        // Assert
        assertThat(actual).isEqualTo(expectedAnalyticsValue)
    }
}