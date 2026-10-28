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

    /**
     * Receives every [TrackingContextProxy] call after the proxy has updated the analytics context. Contribute an
     * implementation via `@IntoSet` and override only the calls it needs.
     *
     * Observers are invoked by the proxy only — inject [TrackingContextProxy] to change the context. Their order is
     * not guaranteed, so an observer must not rely on another one.
     */
    interface Observer : TrackingContextProxy {

        override fun setContext(scanResponse: ScanResponse) = Unit

        override fun setContext(userWallet: UserWallet) = Unit

        override fun addContext(userWallet: UserWallet) = Unit

        override fun setHotWalletContext() = Unit

        override fun eraseContext() = Unit

        override fun addContext(scanResponse: ScanResponse) = Unit

        override fun addHotWalletContext() = Unit

        override fun removeContext() = Unit
    }
}