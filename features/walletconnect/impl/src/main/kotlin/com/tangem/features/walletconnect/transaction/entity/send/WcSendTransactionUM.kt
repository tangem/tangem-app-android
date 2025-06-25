package com.tangem.features.walletconnect.transaction.entity.send

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.walletconnect.transaction.entity.approve.WcSpendAllowanceUM
import com.tangem.features.walletconnect.transaction.entity.blockaid.WcSendReceiveTransactionCheckResultsUM
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionUM
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionFeeState
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM

internal data class WcSendTransactionUM(
    val transaction: WcSendTransactionItemUM,
    override val transactionRequestInfo: WcTransactionRequestInfoUM,
    val spendAllowance: WcSpendAllowanceUM? = null,
) : WcCommonTransactionUM

internal data class WcSendTransactionItemUM(
    val feeState: WcTransactionFeeState,
    val onDismiss: () -> Unit,
    val onSend: () -> Unit,
    val appInfo: WcTransactionAppInfoContentUM,
    val estimatedWalletChanges: WcSendReceiveTransactionCheckResultsUM?,
    val walletName: String,
    val networkInfo: WcNetworkInfoUM,
    val address: String? = null,
    val isLoading: Boolean = false,
) : TangemBottomSheetConfigContent