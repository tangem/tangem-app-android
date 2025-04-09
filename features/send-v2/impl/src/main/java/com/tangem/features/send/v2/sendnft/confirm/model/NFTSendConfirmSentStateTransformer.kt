package com.tangem.features.send.v2.sendnft.confirm.model

import com.tangem.blockchain.common.TransactionData
import com.tangem.features.send.v2.common.ui.state.ConfirmUM
import com.tangem.features.send.v2.sendnft.ui.state.NFTSendUM
import com.tangem.utils.transformer.Transformer

internal class NFTSendConfirmSentStateTransformer(
    private val txData: TransactionData.Uncompiled,
    private val txUrl: String,
) : Transformer<NFTSendUM> {
    override fun transform(prevState: NFTSendUM): NFTSendUM {
        return prevState.copy(
            confirmUM = ConfirmUM.Success(
                transactionDate = txData.date?.timeInMillis ?: System.currentTimeMillis(),
                txUrl = txUrl,
            ),
        )
    }
}