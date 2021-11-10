package com.tangem.tap.common.redux

import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.features.wallet.redux.Currency

/**
[REDACTED_AUTHOR]
 */
interface StateDialog

sealed class AppDialog : StateDialog {
    object ScanFailsDialog : AppDialog()
    data class AddressInfoDialog(
        val currency: Currency,
        val addressData: AddressData,
    ) : AppDialog()
}