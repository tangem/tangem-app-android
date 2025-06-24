package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.utils.transformer.Transformer
import java.math.BigInteger

internal class SendFeeNonceChangeTransformer(
    private val value: String,
) : Transformer<FeeUM> {

    override fun transform(prevState: FeeUM): FeeUM {
        val state = prevState as? FeeUM.Content ?: return prevState

        if (value.isEmpty()) {
            return state.copy(nonce = null)
        }

        return try {
            val nonce = BigInteger(value)
            state.copy(
                nonce = nonce,
                feeSelectorUM = when (val feeSelectorUM = state.feeSelectorUM) {
                    is FeeSelectorUM.Loading,
                    is FeeSelectorUM.Error,
                    -> feeSelectorUM
                    is FeeSelectorUM.Content -> feeSelectorUM.copy(
                        nonce = nonce,
                    )
                },
            )
        } catch (e: NumberFormatException) {
            state
        }
    }
}