package com.tangem.feature.wallet.presentation.tokenlist.entity.transformer

import com.tangem.core.ui.components.fields.entity.SearchBarUM

internal class UpdateSearchBarIntentsTransformer(
    private val onQueryChange: (String) -> Unit,
    private val onActiveChange: (Boolean) -> Unit,
) : SearchBarUMTransformer() {

    override fun transform(prevState: SearchBarUM): SearchBarUM {
        return prevState.copy(onQueryChange = onQueryChange, onActiveChange = onActiveChange)
    }
}
