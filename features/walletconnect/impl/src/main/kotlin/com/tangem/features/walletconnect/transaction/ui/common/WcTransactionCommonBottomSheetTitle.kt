package com.tangem.features.walletconnect.transaction.ui.common

import androidx.compose.runtime.Composable
import com.tangem.core.ui.components.bottomsheets.modal.TangemModalBottomSheetTitle
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.walletconnect.impl.R
import com.tangem.features.walletconnect.transaction.routes.WcTransactionRoutes

@Composable
internal fun WcTransactionCommonBottomSheetTitle(
    route: WcTransactionRoutes,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit,
) {
    TangemModalBottomSheetTitle(
        title = resourceReference(R.string.wc_wallet_connect),
        startIconRes = when (route) {
            is WcTransactionRoutes.Transaction -> null
            else -> R.drawable.ic_back_24
        },
        onStartClick = when (route) {
            is WcTransactionRoutes.Transaction -> null
            else -> onStartClick
        },
        endIconRes = when (route) {
            is WcTransactionRoutes.Transaction -> R.drawable.ic_close_24
            else -> null
        },
        onEndClick = when (route) {
            is WcTransactionRoutes.Transaction -> onEndClick
            else -> null
        },
    )
}