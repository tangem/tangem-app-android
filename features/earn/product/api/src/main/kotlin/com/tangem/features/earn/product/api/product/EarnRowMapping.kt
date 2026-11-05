package com.tangem.features.earn.product.api.product

import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.features.earn.api.ui.EarnRowUM

fun EarnRow.toUM(onInfoClick: (EarnTextBlock) -> Unit): EarnRowUM = EarnRowUM(
    label = label,
    value = when (val rowValue = value) {
        is EarnValue.Text -> rowValue.text
    },
    onInfoClick = info?.let { block -> { onInfoClick(block) } },
)

fun EarnTextBlock.toUM(): TangemModalContentUM.Informer.Item = TangemModalContentUM.Informer.Item(
    title = title,
    description = description,
)