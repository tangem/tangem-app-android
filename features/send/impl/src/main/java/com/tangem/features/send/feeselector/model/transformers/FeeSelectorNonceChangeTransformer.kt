package com.tangem.features.send.feeselector.model.transformers

import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeNonce
import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer

internal class FeeSelectorNonceChangeTransformer(
    private val value: String,
) : Transformer<FeeSelectorUM> {

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        val state = prevState as? FeeSelectorUM.Content ?: return prevState
        val feeNonce = state.feeNonce as? FeeNonce.Nonce ?: return prevState
        if (value.isEmpty()) {
            return state.copy(feeNonce = feeNonce.copy(null))
        }

        val nonce = value.toBigIntegerOrNull() ?: return prevState

        return state.copy(feeNonce = feeNonce.copy(nonce = nonce))
    }
}