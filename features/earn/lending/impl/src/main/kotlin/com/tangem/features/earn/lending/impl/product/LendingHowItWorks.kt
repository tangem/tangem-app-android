package com.tangem.features.earn.lending.impl.product

import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.rate.formatEarnPercent
import java.math.BigDecimal

private const val AUDITOR = "Pessimistic"

internal fun lendingHowItWorks(performanceFee: BigDecimal): EarnHowItWorks = EarnHowItWorks(
    title = resourceReference(R.string.earn_how_it_works_title),
    entries = listOf(
        EarnTextBlock(
            title = resourceReference(R.string.lending_how_it_works_choose_amount_title),
            description = resourceReference(R.string.lending_how_it_works_choose_amount_text),
        ),
        EarnTextBlock(
            title = resourceReference(R.string.lending_how_it_works_money_works_title),
            description = resourceReference(R.string.lending_how_it_works_money_works_text),
        ),
        EarnTextBlock(
            title = resourceReference(R.string.earn_how_it_works_watch_it_grow_title),
            description = resourceReference(
                R.string.lending_how_it_works_watch_it_grow_text,
                wrappedList(performanceFee.formatEarnPercent()),
            ),
        ),
        EarnTextBlock(
            title = resourceReference(R.string.earn_how_it_works_take_it_back_title),
            description = resourceReference(R.string.lending_how_it_works_take_it_back_text),
        ),
    ),
    footnote = resourceReference(R.string.earn_how_it_works_audited_by, wrappedList(AUDITOR)),
    learnMore = TangemBlogUrlBuilder.Post.HowYieldModeWorks,
)