package com.tangem.features.tangempay.withdraw.bank.preview.model

internal data class PreviewState(
    val isSending: Boolean = false,
    val failure: PreviewFailure? = null,
)

/** Create-order outcomes that replace the summary with a full-screen result; only [General] offers Retry. */
internal enum class PreviewFailure { RecipientRegion, SenderRegion, Business, General }