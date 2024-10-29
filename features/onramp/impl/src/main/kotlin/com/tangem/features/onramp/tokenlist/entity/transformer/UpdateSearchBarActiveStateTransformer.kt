package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.onramp.impl.R

internal class UpdateSearchBarActiveStateTransformer(private val isActive: Boolean) : SearchBarUMTransformer() {

    override fun transform(prevState: SearchBarUM): SearchBarUM {
        val placeholderText = if (isActive) {
            TextReference.EMPTY
        } else {
            resourceReference(id = R.string.common_search)
        }

        return prevState.copy(placeholderText = placeholderText, isActive = isActive)
    }
}