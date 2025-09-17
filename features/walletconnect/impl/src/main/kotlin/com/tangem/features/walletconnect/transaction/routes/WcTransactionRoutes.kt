package com.tangem.features.walletconnect.transaction.routes

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed class WcTransactionRoutes : TangemBottomSheetConfigContent, Route {
    @Serializable
    data object Transaction : WcTransactionRoutes()

    @Serializable
    data object TransactionRequestInfo : WcTransactionRoutes()

    @Serializable
    data object CustomAllowance : WcTransactionRoutes()

    @Serializable
    data object SelectFee : WcTransactionRoutes()

    @Serializable
    data class Alert(val type: Type) : WcTransactionRoutes() {
        @Serializable
        sealed class Type {
            data class Verified(val appName: String) : Type()
            data object UnknownDomain : Type()
            data object UnsafeDomain : Type()
            data class BlockAidErrorInfo(
                val description: String?,
                val onClick: () -> Unit,
                val iconType: MessageBottomSheetUMV2.Icon.Type,
                val iconBgType: MessageBottomSheetUMV2.Icon.BackgroundType,
            ) : Type()

            data class UnknownError(
                val errorMessage: String?,
                val onDismiss: () -> Unit,
                val onRetry: () -> Unit,
            ) : Type()
        }
    }

    @Serializable
    data class MultipleTransactions(
        val onConfirm: () -> Unit,
    ) : WcTransactionRoutes()

    @Serializable
    data object TransactionProcess : WcTransactionRoutes()
}