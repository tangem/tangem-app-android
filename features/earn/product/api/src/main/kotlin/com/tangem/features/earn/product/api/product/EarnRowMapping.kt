package com.tangem.features.earn.product.api.product

import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.ui.ds2.modal.TangemModalContentUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.api.ui.EarnHowItWorksUM
import com.tangem.features.earn.api.ui.EarnRowUM
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

fun EarnRow.toUM(
    onInfoClick: (EarnTextBlock) -> Unit,
    onLinkClick: ((String) -> Unit)? = null,
    yearlyFiatEarningsText: (BigDecimal) -> TextReference = { amount -> stringReference(amount.toPlainString()) },
    amountText: (BigDecimal) -> TextReference = { amount -> stringReference(amount.toPlainString()) },
): EarnRowUM = EarnRowUM(
    label = label,
    value = when (val rowValue = value) {
        is EarnValue.Text -> rowValue.text
        is EarnValue.Link -> rowValue.text
        is EarnValue.Amount -> amountText(rowValue.value)
        is EarnValue.YearlyFiatEarnings -> yearlyFiatEarningsText(rowValue.value)
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

fun EarnHowItWorks.toUM(onLearnMoreClick: (TangemBlogUrlBuilder.Post) -> Unit): EarnHowItWorksUM = EarnHowItWorksUM(
    title = title,
    steps = entries.map { entry -> EarnHowItWorksUM.Step(title = entry.title, description = entry.description) }
        .toImmutableList(),
    footnote = footnote,
    onLearnMoreClick = learnMore?.let { post -> { onLearnMoreClick(post) } },
)