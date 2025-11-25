package com.tangem.features.walletconnect.transaction.entity.addresses

import androidx.annotation.DrawableRes
import com.tangem.features.walletconnect.transaction.entity.common.WcNetworkInfoUM
import com.tangem.features.walletconnect.transaction.entity.common.WcTransactionAppInfoContentUM

/**
 * UI model for Bitcoin getAccountAddresses WalletConnect request.
 */
internal data class WcGetAddressesUM(
    val appInfo: WcTransactionAppInfoContentUM,
    val networkInfo: WcNetworkInfoUM,
    val addresses: List<AddressInfo>,
    val isLoading: Boolean,
    @DrawableRes val walletInteractionIcon: Int,
    val onApprove: () -> Unit,
    val onReject: () -> Unit,
) {
    data class AddressInfo(
        val address: String,
        val intention: String?,
    )
}