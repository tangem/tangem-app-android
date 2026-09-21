package com.tangem.features.collectibles.impl.machinedetails.model.transformer

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.core.ui.format.bigdecimal.simple
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.features.collectibles.impl.machinedetails.model.converter.CollectiblesMachineDetailRowsConverter
import com.tangem.features.collectibles.impl.machinedetails.model.converter.CollectiblesMachineTiersConverter
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.utils.transformer.Transformer

internal class CollectiblesUpdateMachineDetailsTransformer(
    val machine: CollectibleGachaMachine,
    val onBackClick: () -> Unit,
    val onBuyClick: () -> Unit,
    val onTurboCheckedChange: (Boolean) -> Unit,
    val onTurboInfoClick: () -> Unit,
) : Transformer<CollectiblesMachineDetailsUM> {
    override fun transform(prevState: CollectiblesMachineDetailsUM): CollectiblesMachineDetailsUM {
        val previousContent = prevState as? CollectiblesMachineDetailsUM.Content

        return CollectiblesMachineDetailsUM.Content(
            onBackClick = onBackClick,
            title = stringReference(machine.shortName.ifBlank { machine.name }),
            imageUrl = machine.imageUrl,
            tiers = CollectiblesMachineTiersConverter.convert(machine),
            details = CollectiblesMachineDetailRowsConverter.convert(machine),
            prizePool = previousContent?.prizePool ?: CollectiblesMachineDetailsUM.PrizePoolUM.Loading,
            footer = CollectiblesMachineDetailsUM.FooterUM(
                isTurboEnabled = previousContent?.footer?.isTurboEnabled == true,
                onTurboCheckedChange = onTurboCheckedChange,
                onTurboInfoClick = onTurboInfoClick,
                expectedValue = stringReference(
                    machine.expectedValue.format {
                        fiat(
                            fiatCurrencyCode = AppCurrency.Default.code,
                            fiatCurrencySymbol = AppCurrency.Default.symbol,
                        ).optionalDecimals()
                    },
                ),
                buyButtonText = stringReference(
                    // TODO replace with actual text when settled
                    "Open for ${machine.price.format { simple(decimals = 2) }} USDC",
                ),
                onBuyClick = onBuyClick,
            ),
        )
    }
}