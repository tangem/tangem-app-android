package com.tangem.features.collectibles.impl.packopening.model.converter

import com.tangem.features.collectibles.impl.common.formatter.formatUsdc
import com.tangem.features.collectibles.impl.packopening.CollectiblesConfirmOrderBottomSheetConfig
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal object CollectiblesAutoBuybackConverter :
    Converter<CollectiblesAutoBuybackConverter.Input, CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback> {

    override fun convert(value: Input) = CollectiblesConfirmOrderBottomSheetConfig.AutoBuyback(
        amount = StringsSigns.PLUS + value.amount.formatUsdc(),
        price = value.price.formatUsdc(minDecimals = 0),
    )

    /** @property price what one more spin costs, null while the machine is unknown */
    data class Input(val amount: BigDecimal, val price: BigDecimal?)
}