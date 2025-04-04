package com.tangem.features.send.v2.send.confirm.model.transformers

import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.send.ui.state.SendUM
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