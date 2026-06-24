package com.tangem.features.send.feeselector.model.transformers

import com.tangem.features.send.api.subcomponents.feeSelector.entity.FeeSelectorUM
import com.tangem.utils.transformer.Transformer

internal object FeeSelectorLoadingTransformer : Transformer<FeeSelectorUM> {

    override fun transform(prevState: FeeSelectorUM): FeeSelectorUM {
        return FeeSelectorUM.Loading
    }
}