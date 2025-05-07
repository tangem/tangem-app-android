package com.tangem.features.walletconnect.transaction.routes

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed class WcSignTransactionRoutes : TangemBottomSheetConfigContent {
    @Serializable
    data object Transaction : WcSignTransactionRoutes()

    @Serializable
    data object TransactionRequestInfo : WcSignTransactionRoutes()
}