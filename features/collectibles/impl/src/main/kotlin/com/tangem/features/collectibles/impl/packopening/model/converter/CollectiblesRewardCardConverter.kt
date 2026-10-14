package com.tangem.features.collectibles.impl.packopening.model.converter

import com.tangem.core.res.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.features.collectibles.impl.common.formatter.formatUsd
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardUM
import com.tangem.utils.StringsSigns.NUMBER_SIGN
import java.math.BigDecimal

/** The won card as the reward shows it. */
internal object CollectiblesRewardCardConverter {

    // TODO([REDACTED_TASK_KEY]): the buy-back offer comes from its own backend call
    private val SELL_BACK_VALUE = BigDecimal("55.80")

    fun convert(pack: OpenedCollectiblePack): RewardCardUM {
        return RewardCardUM(
            rarity = pack.rarity.toLabel(),
            accent = pack.rarity.toAccent(),
            title = listOfNotNull(pack.year, pack.set).joinToString(separator = " "),
            editionNumber = pack.serialNumber.toNumber(),
            marketValue = pack.insuredValue.formatUsd(),
            sellBackValue = SELL_BACK_VALUE.formatUsd(),
            frontImageUrl = pack.imageFrontUrl,
            backImageUrl = pack.imageBackUrl,
        )
    }

    private fun CollectibleRarity?.toLabel(): TextReference = resourceReference(
        when (this) {
            CollectibleRarity.COMMON, null -> R.string.collectibles_open_pack_pull_common
            CollectibleRarity.UNCOMMON -> R.string.collectibles_open_pack_pull_uncommon
            CollectibleRarity.RARE -> R.string.collectibles_open_pack_pull_rare
            CollectibleRarity.EPIC -> R.string.collectibles_open_pack_pull_epic
        },
    )

    private fun CollectibleRarity?.toAccent(): RewardCardUM.Accent = when (this) {
        CollectibleRarity.COMMON, null -> RewardCardUM.Accent.Neutral
        CollectibleRarity.UNCOMMON -> RewardCardUM.Accent.Green
        CollectibleRarity.RARE -> RewardCardUM.Accent.Blue
        CollectibleRarity.EPIC -> RewardCardUM.Accent.Violet
    }

    private fun String?.toNumber(): String = this?.let { "$NUMBER_SIGN$it" }.orEmpty()
}