package com.tangem.features.collectibles.impl.discovery.model.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.features.collectibles.impl.R
import com.tangem.features.collectibles.impl.discovery.ui.state.CollectiblesDiscoveryUM
import com.tangem.utils.converter.Converter

internal class CollectiblesDiscoveryPackConverter(
    private val onPackClick: (String) -> Unit,
) : Converter<CollectibleGachaMachine, CollectiblesDiscoveryUM.PackUM> {

    override fun convert(value: CollectibleGachaMachine): CollectiblesDiscoveryUM.PackUM {
        return CollectiblesDiscoveryUM.PackUM(
            id = value.id,
            imageUrl = value.imageUrl.orEmpty(),
            title = stringReference(value.shortName.ifBlank { value.name }),
            price = stringReference(
                value.price.format {
                    fiat(
                        fiatCurrencyCode = AppCurrency.Default.code,
                        fiatCurrencySymbol = AppCurrency.Default.symbol,
                    ).optionalDecimals()
                },
            ),
            badge = if (value.instantBuybackPercent > 0) {
                TextReference.Res(
                    R.string.collectibles_main_pack_buyback_badge,
                    wrappedList("${value.instantBuybackPercent}%"),
                )
            } else {
                null
            },
            onClick = { onPackClick(value.externalId) },
        )
    }
}