package com.tangem.features.walletconnect.transaction.entity.chain

import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.walletconnect.transaction.entity.common.WcCommonTransactionUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM

internal data class WcAddEthereumChainUM(
    val transaction: WcAddEthereumChainItemUM,
    override val transactionRequestInfo: WcTransactionRequestInfoUM,
) : WcCommonTransactionUM

internal data class WcAddEthereumChainItemUM(
    val onDismiss: () -> Unit,
    val onSign: () -> Unit,
    val appInfo: WcTransactionAppInfoContentUM,
    val portfolioName: AccountTitleUM?,
    val isLoading: Boolean = false,
) : TangemBottomSheetConfigContent