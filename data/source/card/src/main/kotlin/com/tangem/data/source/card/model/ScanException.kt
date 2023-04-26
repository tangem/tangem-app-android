package com.tangem.data.source.card.model

import com.tangem.common.core.TangemError

sealed class ScanException : Exception() {
    object CardForDifferentApp : ScanException()

    object CardNotSupportedByRelease : ScanException()
}

internal class ScanExceptionWrapper(val exception: ScanException) : TangemError(code = 50100) {
    override var customMessage: String = "Unable to scan card"
    override val messageResId: Int? = null
}
