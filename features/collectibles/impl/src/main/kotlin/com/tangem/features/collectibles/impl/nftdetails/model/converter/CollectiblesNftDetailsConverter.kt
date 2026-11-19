package com.tangem.features.collectibles.impl.nftdetails.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryMockSource
import com.tangem.features.collectibles.impl.nftdetails.ui.state.CollectiblesNftDetailsUM
import com.tangem.features.collectibles.impl.nftdetails.ui.state.CollectiblesNftDetailsUM.BuybackUM
import com.tangem.features.collectibles.impl.nftdetails.ui.state.CollectiblesNftDetailsUM.DetailRowUM
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.StringsSigns.NUMBER_SIGN
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

// TODO([REDACTED_TASK_KEY]): move the copy to Lokalise and convert CollectibleNft instead of the mock card
internal object CollectiblesNftDetailsConverter {

    fun convert(card: CollectiblesInventoryMockSource.Card): CollectiblesNftDetailsUM.Content {
        return CollectiblesNftDetailsUM.Content(
            frontImageUrl = card.imageUrl,
            backImageUrl = null,
            title = "${card.year} $NUMBER_SIGN${card.serialNumber} ${card.cardName}",
            subtitle = "${card.gradingCompany} ${card.gradeValue} $DOT ${card.price}",
            details = persistentListOf(
                DetailRowUM(label = stringReference("Grading company"), value = card.gradingCompany),
                DetailRowUM(label = stringReference("Grade"), value = card.gradeValue),
                DetailRowUM(label = stringReference("Grading ID"), value = "$NUMBER_SIGN${card.gradingId}"),
                DetailRowUM(label = stringReference("Insured value"), value = card.price),
            ),
            buyback = convertBuyback(card),
            cardAspect = null,
        )
    }

    private fun convertBuyback(card: CollectiblesInventoryMockSource.Card): BuybackUM? {
        val price = card.buybackPrice ?: return null
        val timeLeft = card.timeLeft?.takeIf { it.isPositive() } ?: return null

        return BuybackUM(
            timeLeft = combinedReference(stringReference("Buyback available for "), convertTimeLeft(timeLeft)),
            buttonText = stringReference("Accept buyback for $price"),
        )
    }

    private fun convertTimeLeft(timeLeft: Duration): TextReference = when {
        timeLeft < 1.minutes -> resourceReference(R.string.collectibles_inventory_timer_under_minute)
        timeLeft < 1.hours -> resourceReference(
            id = R.string.collectibles_inventory_timer_minutes,
            formatArgs = wrappedList(timeLeft.inWholeMinutes),
        )
        else -> timeLeft.toComponents { hours, minutes, _, _ ->
            resourceReference(
                id = R.string.collectibles_inventory_timer_hours,
                formatArgs = wrappedList(hours, minutes),
            )
        }
    }
}