package com.tangem.features.swap.v2.impl.sendviaswap.confirm.model.transformers

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.swap.models.SwapDataModel
import com.tangem.features.swap.v2.impl.common.entity.ConfirmUM
import com.tangem.features.swap.v2.impl.sendviaswap.entity.SendWithSwapUM
import com.tangem.utils.transformer.Transformer

internal class SendWithSwapConfirmSentStateTransformer(
    private val txUrl: String,
    private val timestamp: Long,
    private val provider: ExpressProvider,
    private val swapDataModel: SwapDataModel,
) : Transformer<SendWithSwapUM> {
    override fun transform(prevState: SendWithSwapUM): SendWithSwapUM {
        return prevState.copy(
            confirmUM = ConfirmUM.Success(
                isPrimaryButtonEnabled = true,
                transactionDate = timestamp,
                txUrl = txUrl,
                provider = provider,
                swapDataModel = swapDataModel,
            ),
        )
    }
}