package com.tangem.tap.common.redux

import com.tangem.tap.features.wallet.redux.AddressData

/**
[REDACTED_AUTHOR]
 */
interface StateDialog

sealed class AppDialog : StateDialog {
    object ScanFailsDialog : AppDialog()
    data class AddressInfoDialog(
        val addressData: AddressData,
        val onCopyAddress: () -> Unit,
        val onExploreAddress: () -> Unit
    ) : AppDialog()
}