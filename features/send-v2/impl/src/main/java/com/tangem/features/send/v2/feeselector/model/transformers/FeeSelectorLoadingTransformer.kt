package com.tangem.features.send.v2.feeselector.model.transformers

import com.tangem.features.send.v2.api.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer

internal object FeeSelectorLoadingTransformer : Transformer<FeeSelectorUM> {

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        return FeeSelectorUM.Loading
    }
}