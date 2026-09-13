package com.tangem.features.tangempay.cashback.impl.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import java.math.BigDecimal

internal data class CashbackRateTitles(val title: TextReference, val subtitle: TextReference) {
    companion object {
        internal operator fun invoke(cards: List<CashbackCard>): CashbackRateTitles {
            return when {
                cards.isEmpty() -> CashbackRateTitles(
                    title = resourceReference(R.string.tangempay_cashback_title),
                    subtitle = TextReference.EMPTY,
                )
                cards.size == 1 -> {
                    val cashbackCard = cards.single()
                    CashbackRateTitles(
                        title = resourceReference(
                            id = R.string.tangempay_cashback_rate_title,
                            formatArgs = wrappedList(cashbackCard.rate.formatRate()),
                        ),
                        subtitle = cashbackCard.subtitleOrEmpty(),
                    )
                }
                else -> {
                    val cashbackCard = cards.maxBy { it.rate }
                    CashbackRateTitles(
                        title = resourceReference(
                            id = R.string.tangempay_cashback_rate_title_up_to,
                            formatArgs = wrappedList(cashbackCard.rate.formatRate()),
                        ),
                        subtitle = cashbackCard.subtitleOrEmpty(),
                    )
                }
            }
        }

        private fun CashbackCard.subtitleOrEmpty(): TextReference {
            val cardTitle = title?.takeIf(String::isNotBlank) ?: return TextReference.EMPTY
            return resourceReference(
                id = R.string.tangempay_cashback_rate_subtitle,
                formatArgs = wrappedList(cardTitle),
            )
        }
    }
}

internal fun BigDecimal.formatRate(): String = stripTrailingZeros().toPlainString()