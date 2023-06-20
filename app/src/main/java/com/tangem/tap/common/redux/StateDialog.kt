package com.tangem.tap.common.redux

import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.TestAction
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.wallet.models.Currency

/**
[REDACTED_AUTHOR]
 */
interface StateDialog

sealed class AppDialog : StateDialog {
    data class SimpleOkDialog(val header: String, val message: String, val onOk: VoidCallback? = null) : AppDialog()
    data class SimpleOkErrorDialog(val message: String, val onOk: VoidCallback? = null) : AppDialog()
    data class SimpleOkWarningDialog(val message: String, val onOk: VoidCallback? = null) : AppDialog()
    data class SimpleOkDialogRes(
        val headerId: Int,
        val messageId: Int,
        val onOk: VoidCallback? = null,
    ) : AppDialog()

    data class OkCancelDialogRes(
        val headerId: Int,
        val messageId: Int,
        val okButton: DialogButton,
        val cancelButton: DialogButton,
    ) : AppDialog()

    data class DialogButton(
        val title: Int,
        val action: VoidCallback? = null,
    )

    object ScanFailsDialog : AppDialog()

    data class AddressInfoDialog(
        val currency: Currency,
        val addressData: WalletDataModel.AddressData,
    ) : AppDialog()

    data class TestActionsDialog(
        val actionsList: List<TestAction>,
    ) : AppDialog()
}