package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference

data class EarnRow(
    val label: TextReference,
    val value: EarnValue,
    val info: EarnTextBlock? = null,
)

sealed interface EarnValue {

    data class Text(val text: TextReference) : EarnValue

    /** A value that opens [url] on click, shown with an arrow after the text. */
    data class Link(val text: TextReference, val url: String) : EarnValue
}

/** @property icon the icon of a benefit line; the explanation sheet of a row ignores it */
data class EarnTextBlock(
    val title: TextReference,
    val description: TextReference,
    val icon: TangemIconUM.Icon? = null,
)