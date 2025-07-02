package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer

internal class FeeSelectorErrorTransformer(private val error: GetFeeError) : Transformer<FeeSelectorUM> {

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        return FeeSelectorUM.Error(error = error)
    }
}