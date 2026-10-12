package com.tangem.features.tangempay.withdraw.bank.preview.model

internal data class PreviewState(
    val send: SendState = SendState.Idle,
    val failure: PreviewFailure? = null,
    /** Set when a failure left a live order behind, so Retry resumes that order instead of creating a second one. */
    val resumableOrderId: String? = null,
    /** A cold wallet signs the transfer with a card tap; a hot one signs without a card. */
    val isColdWallet: Boolean = true,
) {

    val isSending: Boolean get() = send != SendState.Idle
}

/** How far the send has got. The screen only needs "busy or not"; the model needs the order it is driving. */
internal sealed interface SendState {

    data object Idle : SendState

    data object CreatingOrder : SendState

    /** The order exists and is polled until the backend asks for a signature. */
    data class AwaitingSignature(val orderId: String) : SendState

    /** The card (or hot wallet) is signing the payload and the signature is being submitted. */
    data class Signing(val orderId: String) : SendState
}

/** Outcomes that replace the summary with a full-screen result; only [General] offers Retry. */
internal enum class PreviewFailure { RecipientRegion, SenderRegion, Business, UnsupportedConfiguration, General }