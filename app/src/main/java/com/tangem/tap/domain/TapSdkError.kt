package com.tangem.tap.domain

import com.tangem.common.core.TangemError
import com.tangem.wallet.R

sealed class TapSdkError(override val messageResId: Int?) : TangemError(code = 50100) {
    override var customMessage: String = code.toString()

    class CardForDifferentApp : TapSdkError(R.string.alert_unsupported_card)
    class CardNotSupportedByRelease : TapSdkError(R.string.error_wrong_card_type)
}