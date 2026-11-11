package com.tangem.features.collectibles.impl.packopening.model.converter

import com.tangem.domain.collectibles.models.OpenedCollectiblePack
import com.tangem.features.collectibles.impl.common.formatter.formatUsdc
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.utils.StringsSigns
import com.tangem.utils.StringsSigns.NUMBER_SIGN
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal object CollectiblesAutoBuybackConverter :
    Converter<CollectiblesAutoBuybackConverter.Input, CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback> {

    override fun convert(value: Input) = CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback(
        amount = StringsSigns.PLUS + value.amount.formatUsdc(),
        price = value.price.formatUsdc(minDecimals = 0),
        cardImageUrl = value.pack?.imageFrontUrl,
        cardTitle = value.pack?.toTitle(),
    )

    private fun OpenedCollectiblePack.toTitle(): String? = listOfNotNull(
        year,
        set,
        serialNumber?.let { "$NUMBER_SIGN$it" },
    )
        .joinToString(separator = " ")
        .ifEmpty { null }

    /** @property price what one more spin costs, null while the machine is unknown */
    data class Input(val amount: BigDecimal, val price: BigDecimal?, val pack: OpenedCollectiblePack?)
}