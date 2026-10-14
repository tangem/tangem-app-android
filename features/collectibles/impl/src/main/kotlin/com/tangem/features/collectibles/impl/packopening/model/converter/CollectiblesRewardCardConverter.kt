package com.tangem.features.collectibles.impl.packopening.model.converter

import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.models.CollectibleRarity
import com.tangem.domain.collectibles.models.OpenedCollectiblePack
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
            marketValue = pack.insuredValue.toFiat(),
            sellBackValue = SELL_BACK_VALUE.toFiat(),
            frontImageUrl = pack.imageFrontUrl,
            backImageUrl = pack.imageBackUrl,
        )
    }

    /** An unrecognised or absent rarity still has to read as *something* the user pulled. */
    private fun CollectibleRarity?.toLabel(): String = when (this) {
        CollectibleRarity.COMMON -> "Common pull"
        CollectibleRarity.UNCOMMON -> "Uncommon pull"
        CollectibleRarity.RARE -> "Rare pull"
        CollectibleRarity.EPIC -> "Epic pull"
        null -> "Your pull"
    }

    private fun CollectibleRarity?.toAccent(): RewardCardUM.Accent = when (this) {
        CollectibleRarity.COMMON, null -> RewardCardUM.Accent.Neutral
        CollectibleRarity.UNCOMMON -> RewardCardUM.Accent.Green
        CollectibleRarity.RARE -> RewardCardUM.Accent.Blue
        CollectibleRarity.EPIC -> RewardCardUM.Accent.Violet
    }

    private fun String?.toNumber(): String = this?.let { "$NUMBER_SIGN$it" }.orEmpty()

    private fun BigDecimal?.toFiat(): String = format {
        fiat(
            fiatCurrencyCode = AppCurrency.Default.code,
            fiatCurrencySymbol = AppCurrency.Default.symbol,
        ).optionalDecimals()
    }
}