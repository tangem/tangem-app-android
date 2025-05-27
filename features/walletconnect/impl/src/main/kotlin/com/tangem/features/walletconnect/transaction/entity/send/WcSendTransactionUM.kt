package com.tangem.features.walletconnect.transaction.entity.send

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcEstimatedWalletChangesUM
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM

internal data class WcSendTransactionUM(
    val transaction: WcSendTransactionItemUM,
    val transactionRequestInfo: WcTransactionRequestInfoUM,
)

internal data class WcSendTransactionItemUM(
    val onDismiss: () -> Unit,
    val onSend: () -> Unit,
    val appInfo: WcTransactionAppInfoContentUM,
    val estimatedWalletChanges: WcEstimatedWalletChangesUM,
    val walletName: String,
    val networkInfo: WcNetworkInfoUM,
    val networkFee: String? = null,
    val address: String? = null,
    val isLoading: Boolean = false,
) : TangemBottomSheetConfigContent