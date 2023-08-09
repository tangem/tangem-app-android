package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem

internal fun getGroupPlaceholder(index: Int): DraggableItem.GroupPlaceholder {
    return DraggableItem.GroupPlaceholder(id = "placeholder_${index.inc()}")
}