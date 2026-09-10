package com.tangem.features.send.send.confirm.model.transformers

import com.tangem.features.send.common.ui.state.ConfirmUM
import com.tangem.features.send.send.ui.state.SendUM
import com.tangem.utils.transformer.Transformer

internal class SendConfirmJointOperationCreatedTransformer(
    private val createdAt: Long,
) : Transformer<SendUM> {

    override fun transform(prevState: SendUM): SendUM {
        return prevState.copy(
            confirmUM = ConfirmUM.Success(
                transactionDate = createdAt,
                txUrl = "",
                signaturesUM = (prevState.confirmUM as? ConfirmUM.Content)?.signaturesUM,
            ),
        )
    }
}