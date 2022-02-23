package com.tangem.tap.common.redux

import com.tangem.tap.features.wallet.redux.AddressData
import com.tangem.tap.features.wallet.redux.Currency

/**
[REDACTED_AUTHOR]
 */
interface StateDialog

sealed class AppDialog : StateDialog {
    data class SimpleOkDialog(val header: String, val message: String) : AppDialog()
    data class SimpleOkDialogRes(val headerId: Int, val messageId: Int) : AppDialog()
    object ScanFailsDialog : AppDialog()
    data class AddressInfoDialog(
        val currency: Currency,
        val addressData: AddressData,
    ) : AppDialog()
}