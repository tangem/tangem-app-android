package com.tangem.features.walletconnect.transaction.routes

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed class WcTransactionRoutes : TangemBottomSheetConfigContent {
    @Serializable
    data object Transaction : WcTransactionRoutes()

    @Serializable
    data object TransactionRequestInfo : WcTransactionRoutes()
}