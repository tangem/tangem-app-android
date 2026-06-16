package com.tangem.features.send.sendnft.confirm.model.transformers

import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.sendnft.ui.state.NFTSendUM
import com.tangem.utils.transformer.Transformer

internal class NFTSendConfirmSendingStateTransformer(
    val isSending: Boolean,
) : Transformer<NFTSendUM> {
    override fun transform(prevState: NFTSendUM): NFTSendUM {
        val confirmUM = prevState.confirmUM as? ConfirmUM.Content ?: return prevState
        return prevState.copy(
            confirmUM = confirmUM.copy(
                isPrimaryButtonEnabled = !isSending,
                isSending = isSending,
            ),
        )
    }
}