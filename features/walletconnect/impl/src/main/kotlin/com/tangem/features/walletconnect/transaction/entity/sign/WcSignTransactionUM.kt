package com.tangem.features.walletconnect.transaction.entity.sign

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionUM
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM

internal data class WcSignTransactionUM(
    val transaction: WcSignTransactionItemUM,
    override val transactionRequestInfo: WcTransactionRequestInfoUM,
) : WcCommonTransactionUM

internal data class WcSignTransactionItemUM(
    val onDismiss: () -> Unit,
    val onSign: () -> Unit,
    val appInfo: WcTransactionAppInfoContentUM,
    val walletName: String?,
    val networkInfo: WcNetworkInfoUM,
    val address: String?,
    val walletInteractionIcon: Int?,
    val isLoading: Boolean = false,
) : TangemBottomSheetConfigContent