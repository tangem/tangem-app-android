package com.tangem.features.onramp.utils

import com.tangem.core.ui.components.fields.entity.SearchBarUM

internal class UpdateSearchQueryTransformer(private val newQuery: String) : SearchBarUMTransformer() {

    override fun transform(prevState: SearchBarUM): SearchBarUM {
        return prevState.copy(query = newQuery)
    }
}