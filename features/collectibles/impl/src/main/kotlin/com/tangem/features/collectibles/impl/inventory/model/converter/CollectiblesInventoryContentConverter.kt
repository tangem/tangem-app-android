package com.tangem.features.collectibles.impl.inventory.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryMockSource
import com.tangem.features.collectibles.impl.inventory.model.CollectiblesInventoryTab
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM.ContentUM
import com.tangem.features.collectibles.impl.inventory.ui.state.CollectiblesInventoryUM.ItemUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

internal class CollectiblesInventoryContentConverter(
    private val onCardClick: (cardId: String) -> Unit,
    private val onReloadClick: () -> Unit,
) {

    fun convert(tab: CollectiblesInventoryTab, cards: InventoryCards, isReloading: Boolean): ContentUM {
        return cards?.fold(
            ifLeft = {
                ContentUM.Error(
                    message = resourceReference(R.string.collectibles_inventory_load_error),
                    isReloading = isReloading,
                    onReloadClick = onReloadClick,
                )
            },
            ifRight = { loadedCards ->
                if (loadedCards.isEmpty()) {
                    ContentUM.Empty(message = convertEmptyMessage(tab))
                } else {
                    ContentUM.Items(items = convertItems(tab = tab, cards = loadedCards))
                }
            },
        ) ?: ContentUM.Loading(info = convertInfo(tab))
    }

    fun convertSearch(cards: List<CollectiblesInventoryMockSource.Card>): ContentUM {
        return if (cards.isEmpty()) {
            ContentUM.Empty(message = resourceReference(R.string.collectibles_main_search_empty))
        } else {
            ContentUM.Items(items = cards.map(::convertCard).toImmutableList())
        }
    }

    private fun convertEmptyMessage(tab: CollectiblesInventoryTab) = when (tab) {
        CollectiblesInventoryTab.Buyback -> resourceReference(R.string.collectibles_inventory_buyback_empty)
        CollectiblesInventoryTab.Collection -> resourceReference(R.string.collectibles_inventory_collection_empty)
    }

    private fun convertItems(
        tab: CollectiblesInventoryTab,
        cards: List<CollectiblesInventoryMockSource.Card>,
    ): ImmutableList<ItemUM> {
        return (listOfNotNull(convertInfo(tab)) + cards.map(::convertCard)).toImmutableList()
    }

    private fun convertInfo(tab: CollectiblesInventoryTab) = when (tab) {
        CollectiblesInventoryTab.Buyback -> ItemUM.Info(
            text = resourceReference(R.string.collectibles_inventory_buyback_description),
        )
        CollectiblesInventoryTab.Collection -> null
    }

    private fun convertCard(card: CollectiblesInventoryMockSource.Card) = ItemUM.Card(
        id = card.id,
        imageUrl = card.imageUrl,
        title = stringReference(card.title),
        price = stringReference(card.price),
        grade = stringReference(card.grade),
        timeLeft = card.timeLeft?.let(::convertTimeLeft),
        onClick = { onCardClick(card.id) },
    )

    private fun convertTimeLeft(timeLeft: Duration): TextReference = when {
        timeLeft <= Duration.ZERO -> resourceReference(R.string.collectibles_inventory_timer_expired)
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