package com.tangem.tap.common.redux

/**
[REDACTED_AUTHOR]
 */
interface StateDialog

sealed class AppDialog : StateDialog {
    object ScanFailsDialog : AppDialog()
}