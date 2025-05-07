package com.tangem.features.walletconnect.transaction.entity.approve

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionRequestInfoUM

@Immutable
internal data class WcApproveTransactionUM(
    val transaction: WcApproveTransactionItemUM,
    val transactionRequestInfo: WcTransactionRequestInfoUM,
    val customAllowance: WcCustomAllowanceUM,
)

@Immutable
internal data class WcApproveTransactionItemUM(
    val onDismiss: () -> Unit,
    val onSend: () -> Unit,
    val appInfo: WcTransactionAppInfoContentUM,
    val spendAllowance: WcSpendAllowanceUM? = null,
    val walletName: String,
    val networkInfo: WcNetworkInfoUM,
    val networkFee: String? = null,
    val isLoading: Boolean = false,
) : TangemBottomSheetConfigContent

@Immutable
internal data class WcCustomAllowanceUM(
    @DrawableRes val networkIconRes: Int,
    val tokenIconUrl: String,
    val amountText: String,
    val isUnlimited: Boolean,
)