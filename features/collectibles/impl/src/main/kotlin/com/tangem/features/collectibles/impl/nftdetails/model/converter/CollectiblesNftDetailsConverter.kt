package com.tangem.features.collectibles.impl.nftdetails.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
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

internal object CollectiblesNftDetailsConverter {

    fun convert(card: CollectiblesInventoryMockSource.Card): CollectiblesNftDetailsUM.Content {
        return CollectiblesNftDetailsUM.Content(
            frontImageUrl = card.imageUrl,
            backImageUrl = null,
            title = "${card.year} $NUMBER_SIGN${card.serialNumber} ${card.cardName}",
            subtitle = "${card.gradingCompany} ${card.gradeValue} $DOT ${card.price}",
            details = persistentListOf(
                DetailRowUM(
                    label = resourceReference(R.string.collectibles_card_details_grading_company),
                    value = card.gradingCompany,
                ),
                DetailRowUM(
                    label = resourceReference(R.string.collectibles_card_details_grade),
                    value = card.gradeValue,
                ),
                DetailRowUM(
                    label = resourceReference(R.string.collectibles_card_details_grading_id),
                    value = "$NUMBER_SIGN${card.gradingId}",
                ),
                DetailRowUM(
                    label = resourceReference(R.string.collectibles_card_details_insured_value),
                    value = card.price,
                ),
            ),
            buyback = convertBuyback(card),
            cardAspect = null,
        )
    }

    private fun convertBuyback(card: CollectiblesInventoryMockSource.Card): BuybackUM? {
        val price = card.buybackPrice ?: return null
        val timeLeft = card.timeLeft?.takeIf { it.isPositive() } ?: return null

        return BuybackUM(
            timeLeft = convertTimeLeft(timeLeft),
            buttonText = resourceReference(
                id = R.string.collectibles_card_details_accept_buyback_button,
                formatArgs = wrappedList(price),
            ),
        )
    }

    private fun convertTimeLeft(timeLeft: Duration): TextReference = when {
        timeLeft < 1.minutes -> resourceReference(R.string.collectibles_card_details_buyback_available_under_minute)
        timeLeft < 1.hours -> resourceReference(
            id = R.string.collectibles_card_details_buyback_available_minutes,
            formatArgs = wrappedList(timeLeft.inWholeMinutes),
        )
        else -> timeLeft.toComponents { hours, minutes, _, _ ->
            resourceReference(
                id = R.string.collectibles_card_details_buyback_available_hours,
                formatArgs = wrappedList(hours, minutes),
            )
        }
    }
}