package com.tangem.features.hotwallet.addexistingwallet.entry.routing

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

internal sealed class AddExistingWalletRoute : Route {

    @Serializable
    object Start : AddExistingWalletRoute()

    @Serializable
    object Import : AddExistingWalletRoute()

    @Serializable
    data class BackupCompleted(val userWalletId: UserWalletId) : AddExistingWalletRoute()

    @Serializable
    data class SetAccessCode(val userWalletId: UserWalletId) : AddExistingWalletRoute()

    @Serializable
    data class ConfirmAccessCode(val userWalletId: UserWalletId, val accessCode: String) : AddExistingWalletRoute()

    @Serializable
    object PushNotifications : AddExistingWalletRoute()

    @Serializable
    object SetupFinished : AddExistingWalletRoute()
}