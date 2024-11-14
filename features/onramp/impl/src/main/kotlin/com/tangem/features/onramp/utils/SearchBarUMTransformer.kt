package com.tangem.features.onramp.utils

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.utils.transformer.Transformer

/**
 * Base [SearchBarUM] transformer
 *
[REDACTED_AUTHOR]
 */
internal abstract class SearchBarUMTransformer : Transformer<SearchBarUM> {

    abstract override fun transform(prevState: SearchBarUM): SearchBarUM
}