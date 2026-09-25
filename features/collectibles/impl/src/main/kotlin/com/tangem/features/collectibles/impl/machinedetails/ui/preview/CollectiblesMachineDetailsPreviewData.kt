package com.tangem.features.collectibles.impl.machinedetails.ui.preview

import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

internal object CollectiblesMachineDetailsPreviewData {

    val prize = CollectiblesMachineDetailsUM.PrizeUM(
        id = "nft_1",
        imageUrl = "",
        grade = stringReference("PSA9"),
        title = stringReference("Raging Bolt EX"),
        value = stringReference("$4,000"),
    )

    val content = CollectiblesMachineDetailsUM.Content(
        onBackClick = {},
        title = stringReference("Elite pack"),
        imageUrl = "",
        tiers = persistentListOf(
            tier(id = "common", name = "Common", accent = Accent.Neutral, odds = "$15-$40 · 80%"),
            tier(id = "uncommon", name = "Uncommon", accent = Accent.Green, odds = "$60-$110 · 15%"),
            tier(id = "rare", name = "Rare", accent = Accent.Blue, odds = "$110-$250 · 4%"),
            tier(id = "epic", name = "Epic", accent = Accent.Violet, odds = "$250+ · 1%"),
        ),
        details = persistentListOf(
            CollectiblesMachineDetailsUM.DetailUM(
                id = "contains",
                label = stringReference("Pack contains"),
                value = stringReference("1 card"),
            ),
            CollectiblesMachineDetailsUM.DetailUM(
                id = "buyback",
                label = stringReference("Instant buyback"),
                value = stringReference("85% of value"),
            ),
        ),
        prizePool = prizePool(
            prize,
            prize.copy(id = "nft_2", title = stringReference("Charizard V")),
            prize.copy(id = "nft_3", title = stringReference("Pikachu Illustrator")),
            prize.copy(id = "nft_4", title = stringReference("Umbreon VMAX")),
        ),
        footer = CollectiblesMachineDetailsUM.FooterUM(
            isTurboEnabled = false,
            onTurboCheckedChange = {},
            onTurboInfoClick = {},
            expectedValue = stringReference("$53.95"),
            buyButtonText = stringReference("Open for 50 USDC"),
            onBuyClick = {},
        ),
    )

    val loading = CollectiblesMachineDetailsUM.Loading(onBackClick = {})

    val error = CollectiblesMachineDetailsUM.Error(
        onBackClick = {},
        message = stringReference("Failed to load the pack.\nTap to reload"),
        onReloadClick = {},
    )

    val poolLoading = content.copy(prizePool = CollectiblesMachineDetailsUM.PrizePoolUM.Loading)

    val poolError = content.copy(
        prizePool = CollectiblesMachineDetailsUM.PrizePoolUM.Error(
            message = stringReference("Failed to load the prize pool.\nTap to reload"),
            onReloadClick = {},
        ),
    )

    val poolEmpty = content.copy(
        prizePool = CollectiblesMachineDetailsUM.PrizePoolUM.Empty(
            message = stringReference("This pack has no cards in its prize pool yet"),
        ),
    )

    /** A pack whose backend carries no epic tier, so the odds grid is three cards wide. */
    val withoutEpicTier = content.copy(tiers = content.tiers.dropLast(1).toImmutableList())

    private fun prizePool(vararg prizes: CollectiblesMachineDetailsUM.PrizeUM) =
        CollectiblesMachineDetailsUM.PrizePoolUM.Content(
            rows = prizes.toList()
                .chunked(size = 2) { chunk ->
                    CollectiblesMachineDetailsUM.PrizeRowUM(id = chunk.first().id, prizes = chunk.toImmutableList())
                }
                .toImmutableList(),
            isLoadingNextPage = false,
        )

    private fun tier(id: String, name: String, accent: Accent, odds: String) = CollectiblesMachineDetailsUM.TierUM(
        id = id,
        name = stringReference(name),
        accent = accent,
        odds = stringReference(odds),
    )
}

private typealias Accent = CollectiblesMachineDetailsUM.TierUM.Accent