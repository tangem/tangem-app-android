package com.tangem.features.onramp.utils

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.TextReference

internal class UpdateSearchBarActiveStateTransformer(
    private val isActive: Boolean,
    private val placeHolder: TextReference,
) : SearchBarUMTransformer() {

    override fun transform(prevState: SearchBarUM): SearchBarUM {
        return prevState.copy(placeholderText = if (isActive) TextReference.EMPTY else placeHolder, isActive = isActive)
    }
}