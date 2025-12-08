package com.tangem.core.analytics.utils

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet

/**
[REDACTED_AUTHOR]
 */
interface TrackingContextProxy {

    fun setContext(scanResponse: ScanResponse)

    fun setContext(userWallet: UserWallet)

    fun addContext(userWallet: UserWallet)

    fun setHotWalletContext()

    fun eraseContext()

    fun addContext(scanResponse: ScanResponse)

    fun addHotWalletContext()

    fun removeContext()

    fun proceedWithContext(userWallet: UserWallet, action: () -> Unit)
}