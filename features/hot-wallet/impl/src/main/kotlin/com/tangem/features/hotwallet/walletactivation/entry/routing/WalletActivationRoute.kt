package com.tangem.features.hotwallet.walletactivation.entry.routing

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

internal sealed class WalletActivationRoute : Route {

    @Serializable
    object ManualBackupStart : WalletActivationRoute()

    @Serializable
    object ManualBackupPhrase : WalletActivationRoute()

    @Serializable
    data object ManualBackupCheck : WalletActivationRoute()

    @Serializable
    object ManualBackupCompleted : WalletActivationRoute()

    @Serializable
    data object SetAccessCode : WalletActivationRoute()

    @Serializable
    data class ConfirmAccessCode(val accessCode: String) : WalletActivationRoute()

    @Serializable
    object PushNotifications : WalletActivationRoute()

    @Serializable
    object SetupFinished : WalletActivationRoute()
}