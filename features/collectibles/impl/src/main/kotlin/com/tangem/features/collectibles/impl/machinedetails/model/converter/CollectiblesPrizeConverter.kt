package com.tangem.features.collectibles.impl.machinedetails.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.models.CollectibleGachaMachineNft
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.utils.converter.Converter

internal object CollectiblesPrizeConverter :
    Converter<CollectibleGachaMachineNft, CollectiblesMachineDetailsUM.PrizeUM> {

    override fun convert(value: CollectibleGachaMachineNft): CollectiblesMachineDetailsUM.PrizeUM {
        return CollectiblesMachineDetailsUM.PrizeUM(
            id = value.id,
            imageUrl = value.imageUrl,
            grade = value.grade?.let { stringReference("${it.company}${it.value}") },
            title = stringReference(value.name),
            value = stringReference(
                value.insuredValue.format {
                    fiat(
                        fiatCurrencyCode = AppCurrency.Default.code,
                        fiatCurrencySymbol = AppCurrency.Default.symbol,
                    ).optionalDecimals()
                },
            ),
        )
    }
}