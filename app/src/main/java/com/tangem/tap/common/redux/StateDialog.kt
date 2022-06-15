package com.tangem.tap.common.redux

import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.TestAction
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.AddressData

/**
[REDACTED_AUTHOR]
 */
interface StateDialog

sealed class AppDialog : StateDialog {
    data class SimpleOkDialog(val header: String, val message: String, val onOk: VoidCallback? = null) : AppDialog()
    data class SimpleOkDialogRes(
        val headerId: Int,
        val messageId: Int,
        val onOk: VoidCallback? = null
    ) : AppDialog()

    object ScanFailsDialog : AppDialog()
    data class AddressInfoDialog(
        val currency: Currency,
        val addressData: AddressData,
    ) : AppDialog()

    data class TestActionsDialog(
        val actionsList: List<TestAction>
    ) : AppDialog()
}