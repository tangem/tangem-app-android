package com.tangem.feature.wallet.presentation.tokenlist.entity.transformer

import com.tangem.core.ui.components.fields.entity.SearchBarUM

internal class UpdateSearchQueryTransformer(private val newQuery: String) : SearchBarUMTransformer() {

    override fun transform(prevState: SearchBarUM): SearchBarUM {
        return prevState.copy(query = newQuery)
    }
}