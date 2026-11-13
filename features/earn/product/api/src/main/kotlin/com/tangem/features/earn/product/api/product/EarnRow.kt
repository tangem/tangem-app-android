package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import java.math.BigDecimal

data class EarnRow(
    val label: TextReference,
    val value: EarnValue,
    val info: EarnTextBlock? = null,
)

sealed interface EarnValue {

    data class Text(val text: TextReference) : EarnValue

    /** A value that opens [url] on click, shown with an arrow after the text. */
    data class Link(val text: TextReference, val url: String) : EarnValue

    /**
     * The user's money in the currency of the position: the screen formats it and decides whether to hide it, so a
     * product never turns money into a [Text].
     */
    data class Amount(val value: BigDecimal) : EarnValue
}

/** @property icon the icon of a benefit line; the explanation sheet of a row ignores it */
data class EarnTextBlock(
    val title: TextReference,
    val description: TextReference,
    val icon: TangemIconUM.Icon? = null,
)