package com.tangem.features.walletconnect.transaction.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class WcSignTransactionUM(
    @DrawableRes val startIconRes: Int,
    @DrawableRes val endIconRes: Int,
    @DrawableRes val transactionIconRes: Int,
    val actions: WcTransactionActionsUM,
    val state: State = State.TRANSACTION,
    val transaction: WcTransactionUM,
    val transactionRequestInfo: WcTransactionRequestInfoUM,
) : TangemBottomSheetConfigContent {

    enum class State {
        TRANSACTION, TRANSACTION_REQUEST_INFO
    }
}

@Immutable
internal data class WcTransactionUM(
    val appName: String,
    val appIcon: String,
    val isVerified: Boolean,
    val appSubtitle: String,
    val walletName: String,
    val networkInfo: WcNetworkInfoUM,
    val isLoading: Boolean = false,
)

@Immutable
internal data class WcTransactionRequestInfoUM(
    val info: ImmutableList<WcTransactionRequestInfoItemUM>,
)

@Immutable
internal data class WcTransactionRequestInfoItemUM(
    val title: TextReference,
    val description: String,
)

@Immutable
internal data class WcTransactionActionsUM(
    val transactionRequestOnClick: () -> Unit,
    val onDismiss: () -> Unit,
    val onSign: () -> Unit,
    val onBack: () -> Unit,
    val onCopy: () -> Unit,
)

@Immutable
internal data class WcNetworkInfoUM(
    val name: String,
    @DrawableRes val iconRes: Int,
)