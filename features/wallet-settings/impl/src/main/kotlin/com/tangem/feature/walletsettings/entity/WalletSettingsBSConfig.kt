package com.tangem.feature.walletsettings.entity

import kotlinx.serialization.Serializable

@Serializable
internal sealed interface WalletSettingsBSConfig {

    /** Choice of which type of account to add — a crypto account or a joint account */
    @Serializable
    data object AddAccountType : WalletSettingsBSConfig

    /** List of networks that support notifications */
    @Serializable
    data object NetworksAvailableForNotifications : WalletSettingsBSConfig
}