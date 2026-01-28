package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2

internal sealed class TangemPayViewPinUM {

    open val onDismiss: () -> Unit = {}

    data class Loading(
        override val onDismiss: () -> Unit,
    ) : TangemPayViewPinUM()

    data class Content(
        val pin: String,
        val onClickChangePin: () -> Unit,
        override val onDismiss: () -> Unit,
    ) : TangemPayViewPinUM()

    data class Error(
        val errorMessage: MessageBottomSheetUMV2,
        override val onDismiss: () -> Unit,
    ) : TangemPayViewPinUM()
}