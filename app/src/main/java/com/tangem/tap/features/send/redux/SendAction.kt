package com.tangem.tap.features.send.redux

import org.rekotlin.Action

/**
[REDACTED_AUTHOR]
 */
interface SendAction : Action

sealed class FeeLayout : SendAction {
    object ToggleFeeLayoutVisibility : FeeLayout()
    data class ChangeSelectedFee(val id: Int) : FeeLayout()
    class ChangeIncludeFee(val isChecked: Boolean) : FeeLayout()
}