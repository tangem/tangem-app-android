package com.tangem.features.walletconnect.transaction.routes

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed class WcTransactionRoutes : TangemBottomSheetConfigContent, Route {
    @Serializable
    data object Transaction : WcTransactionRoutes()

    @Serializable
    data object TransactionRequestInfo : WcTransactionRoutes()

    @Serializable
    data class Alert(val type: Type) : WcTransactionRoutes() {
        @Serializable
        sealed class Type {
            data class Verified(val appName: String) : Type()
            data object UnknownDomain : Type()
            data object UnsafeDomain : Type()
        }
    }
}