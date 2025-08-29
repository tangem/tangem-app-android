package com.tangem.features.tokenreceive.route

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Immutable
@Serializable
internal sealed interface TokenReceiveRoutes : Route {

    @Serializable
    data object Warning : TokenReceiveRoutes

    @Serializable
    data object ReceiveAssets : TokenReceiveRoutes

    @Serializable
    data class QrCode(val addressId: Int) : TokenReceiveRoutes
}