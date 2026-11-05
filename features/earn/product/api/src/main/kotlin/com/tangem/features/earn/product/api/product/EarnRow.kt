package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.extensions.TextReference

data class EarnRow(
    val label: TextReference,
    val value: EarnValue,
    val info: EarnTextBlock? = null,
)

sealed interface EarnValue {

    data class Text(val text: TextReference) : EarnValue
}

data class EarnTextBlock(val title: TextReference, val description: TextReference)