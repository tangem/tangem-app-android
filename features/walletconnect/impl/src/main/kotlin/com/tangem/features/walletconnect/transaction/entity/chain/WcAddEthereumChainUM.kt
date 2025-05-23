package com.tangem.features.walletconnect.transaction.entity.chain

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM

internal data class WcAddEthereumChainUM(
    val transaction: WcAddEthereumChainItemUM,
    val transactionRequestInfo: WcTransactionRequestInfoUM,
)

internal data class WcAddEthereumChainItemUM(
    val onDismiss: () -> Unit,
    val onSign: () -> Unit,
    val appInfo: WcTransactionAppInfoContentUM,
    val walletName: String,
    val isLoading: Boolean = false,
) : TangemBottomSheetConfigContent