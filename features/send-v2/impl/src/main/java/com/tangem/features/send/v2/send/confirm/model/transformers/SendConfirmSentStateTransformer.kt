package com.tangem.features.send.v2.send.confirm.model.transformers

import com.tangem.blockchain.common.TransactionData
import com.tangem.features.send.v2.send.confirm.ui.state.ConfirmUM
import com.tangem.features.send.v2.send.ui.state.SendUM
import com.tangem.utils.transformer.Transformer

internal class SendConfirmSentStateTransformer(
    val txData: TransactionData.Uncompiled,
    val txUrl: String,
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