package com.tangem.features.onramp.utils

import com.tangem.core.ui.components.fields.entity.SearchBarUM

internal class UpdateSearchBarCallbacksTransformer(
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
) : SearchBarUMTransformer() {

    override fun transform(prevState: SearchBarUM): SearchBarUM {
        return prevState.copy(onQueryChange = onQueryChange, onActiveChange = onActiveChange)
    }
}