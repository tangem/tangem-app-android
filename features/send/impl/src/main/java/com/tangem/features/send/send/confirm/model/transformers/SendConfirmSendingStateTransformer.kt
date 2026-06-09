package com.tangem.features.send.send.confirm.model.transformers

import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.utils.transformer.Transformer

internal class SendConfirmSendingStateTransformer(
    val isSending: Boolean,
) : Transformer<SendUM> {
    override fun transform(prevState: SendUM): SendUM {
        val confirmUM = prevState.confirmUM as? ConfirmUM.Content ?: return prevState
        return prevState.copy(
            confirmUM = confirmUM.copy(
                isPrimaryButtonEnabled = !isSending,
                isSending = isSending,
            ),
        )
    }
}