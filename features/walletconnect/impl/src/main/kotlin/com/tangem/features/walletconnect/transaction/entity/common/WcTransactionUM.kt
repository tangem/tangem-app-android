package com.tangem.features.walletconnect.transaction.entity.common

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.walletconnect.transaction.entity.approve.SpendAllowanceUM

@Immutable
internal data class WcTransactionUM(
    val appName: String,
    val appIcon: String,
    val isVerified: Boolean,
    val appSubtitle: String,
    val walletName: String,
    val networkInfo: WcNetworkInfoUM,
    val activeButtonText: TextReference,
    val addressText: String? = null,
    val networkFee: String? = null,
    val spendAllowance: SpendAllowanceUM? = null,
    val isLoading: Boolean = false,
)

@Immutable
internal data class WcTransactionActionsUM(
    val transactionRequestOnClick: () -> Unit,
    val onDismiss: () -> Unit,
    val activeButtonOnClick: () -> Unit,
    val onBack: () -> Unit,
    val onCopy: () -> Unit,
)

@Immutable
internal data class WcNetworkInfoUM(
    val name: String,
    @DrawableRes val iconRes: Int,
)