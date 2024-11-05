package com.tangem.features.onramp.utils

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.utils.transformer.Transformer

/**
 * Base [SearchBarUM] transformer
 *
 * @author Andrew Khokhlov on 23/10/2024
 */
internal abstract class SearchBarUMTransformer : Transformer<SearchBarUM> {

    abstract override fun transform(prevState: SearchBarUM): SearchBarUM
}
