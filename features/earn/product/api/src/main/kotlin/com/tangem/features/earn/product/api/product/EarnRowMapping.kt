package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.api.ui.EarnRowUM
import java.math.BigDecimal

fun EarnRow.toUM(
    onInfoClick: (EarnTextBlock) -> Unit,
    onLinkClick: ((String) -> Unit)? = null,
    amountText: (BigDecimal) -> TextReference = { amount -> stringReference(amount.toPlainString()) },
): EarnRowUM = EarnRowUM(
    label = label,
    value = when (val rowValue = value) {
        is EarnValue.Text -> rowValue.text
        is EarnValue.Link -> rowValue.text
        is EarnValue.Amount -> amountText(rowValue.value)
    },
    onInfoClick = info?.let { block -> { onInfoClick(block) } },
    onValueClick = value.linkClick(onLinkClick),
)

private fun EarnValue.linkClick(onLinkClick: ((String) -> Unit)?): (() -> Unit)? {
    val link = this as? EarnValue.Link ?: return null
    val openLink = onLinkClick ?: return null
    return { openLink(link.url) }
}

fun EarnTextBlock.toUM(): TangemModalContentUM.Informer.Item = TangemModalContentUM.Informer.Item(
    title = title,
    description = description,
)