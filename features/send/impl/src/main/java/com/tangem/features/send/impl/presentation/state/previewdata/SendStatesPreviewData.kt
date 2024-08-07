package com.tangem.features.send.impl.presentation.state.previewdata

import com.tangem.common.ui.amountScreen.preview.AmountStatePreviewData
import com.tangem.core.ui.event.consumedEvent
import com.tangem.features.send.impl.presentation.state.SendUiState

internal object SendStatesPreviewData {

    val uiState = SendUiState(
        clickIntents = SendClickIntentsStub,
        isEditingDisabled = false,
        cryptoCurrencyName = "",
        amountState = AmountStatePreviewData.amountWithValueState,
        recipientState = RecipientStatePreviewData.recipientAddressState,
        feeState = FeeStatePreviewData.feeChoosableState,
        sendState = ConfirmStatePreviewData.sendState,
        editAmountState = AmountStatePreviewData.amountWithValueState,
        editRecipientState = RecipientStatePreviewData.recipientAddressState,
        editFeeState = FeeStatePreviewData.feeChoosableState,
        isBalanceHidden = false,
        isSubtracted = false,
        event = consumedEvent(),
    )
}