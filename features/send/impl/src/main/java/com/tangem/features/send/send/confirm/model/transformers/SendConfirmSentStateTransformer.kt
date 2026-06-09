package com.tangem.features.send.send.confirm.model.transformers

import com.tangem.blockchain.common.TransactionData
import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.utils.transformer.Transformer

internal class SendConfirmSentStateTransformer(
    private val txData: TransactionData.Uncompiled,
    private val txUrl: String,
) : Transformer<SendUM> {
    override fun transform(prevState: SendUM): SendUM {
        return prevState.copy(
            confirmUM = ConfirmUM.Success(
                transactionDate = txData.date?.timeInMillis ?: System.currentTimeMillis(),
                txUrl = txUrl,
            ),
        )
    }
}