package com.tangem.features.hotwallet.updateaccesscode.routing

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

internal sealed class UpdateAccessCodeRoute : Route {

    @Serializable
    data class SetAccessCode(val userWalletId: UserWalletId) : UpdateAccessCodeRoute()

    @Serializable
    data class ConfirmAccessCode(val userWalletId: UserWalletId, val accessCode: String) : UpdateAccessCodeRoute()
}