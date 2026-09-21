package com.tangem.features.collectibles.impl.machinedetails.model.converter

import androidx.annotation.StringRes
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.StringsSigns.PERCENT
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/**
 * Builds the "Pack value and odds" cards.
 *
 * A machine whose top tier is absent — no [CollectibleRarity.EPIC] odds or range — renders three cards.
 */
internal object CollectiblesMachineTiersConverter :
    Converter<CollectibleGachaMachine, ImmutableList<CollectiblesMachineDetailsUM.TierUM>> {

    private val TIERS = listOf(
        Tier(
            rarity = CollectibleRarity.COMMON,
            labelRes = R.string.collectibles_machine_tier_common,
            accent = CollectiblesMachineDetailsUM.TierUM.Accent.Neutral,
            readOdds = { it.common },
            readRange = { it.common },
        ),
        Tier(
            rarity = CollectibleRarity.UNCOMMON,
            labelRes = R.string.collectibles_machine_tier_uncommon,
            accent = CollectiblesMachineDetailsUM.TierUM.Accent.Green,
            readOdds = { it.uncommon },
            readRange = { it.uncommon },
        ),
        Tier(
            rarity = CollectibleRarity.RARE,
            labelRes = R.string.collectibles_machine_tier_rare,
            accent = CollectiblesMachineDetailsUM.TierUM.Accent.Blue,
            readOdds = { it.rare },
            readRange = { it.rare },
        ),
        Tier(
            rarity = CollectibleRarity.EPIC,
            labelRes = R.string.collectibles_machine_tier_epic,
            accent = CollectiblesMachineDetailsUM.TierUM.Accent.Violet,
            readOdds = { it.epic },
            readRange = { it.epic },
        ),
    )

    override fun convert(value: CollectibleGachaMachine): ImmutableList<CollectiblesMachineDetailsUM.TierUM> {
        return TIERS.mapNotNull { tier -> tier.toUM(machine = value) }.toImmutableList()
    }

    private fun Tier.toUM(machine: CollectibleGachaMachine): CollectiblesMachineDetailsUM.TierUM? {
        val odds = readOdds(machine.odds) ?: return null
        val range = readRange(machine.tierRanges) ?: return null

        return CollectiblesMachineDetailsUM.TierUM(
            id = rarity.name.lowercase(),
            name = resourceReference(labelRes),
            accent = accent,
            odds = stringReference("${range.format()} $DOT $odds$PERCENT"),
        )
    }

    private fun CollectibleGachaMachine.TierRange.format(): String {
        return "${start.toFiat()}${StringsSigns.MINUS}${end.toFiat()}"
    }

    private fun Int.toFiat(): String = BigDecimal(this).format {
        fiat(
            fiatCurrencyCode = AppCurrency.Default.code,
            fiatCurrencySymbol = AppCurrency.Default.symbol,
        ).optionalDecimals()
    }

    private data class Tier(
        val rarity: CollectibleRarity,
        @param:StringRes val labelRes: Int,
        val accent: CollectiblesMachineDetailsUM.TierUM.Accent,
        val readOdds: (CollectibleGachaMachine.Odds) -> Int?,
        val readRange: (CollectibleGachaMachine.TierRanges) -> CollectibleGachaMachine.TierRange?,
    )
}