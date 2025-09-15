package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers

import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.utils.transformer.Transformer

internal class SendWithSwapConfirmSendingStateTransformer(
    private val isTransactionInProcess: Boolean,
) : Transformer<SendWithSwapUM> {
    override fun transform(prevState: SendWithSwapUM): SendWithSwapUM {
        val confirmUM = prevState.confirmUM as? ConfirmUM.Content ?: return prevState
        return prevState.copy(
            confirmUM = confirmUM.copy(
                isPrimaryButtonEnabled = !isTransactionInProcess,
                isTransactionInProcess = isTransactionInProcess,
            ),
        )
    }
}