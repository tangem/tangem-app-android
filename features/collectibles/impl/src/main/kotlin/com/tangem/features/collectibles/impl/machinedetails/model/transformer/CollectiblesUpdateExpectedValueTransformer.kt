package com.tangem.features.collectibles.impl.machinedetails.model.transformer

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.collectibles.models.CollectibleGachaMachine
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM
import com.tangem.utils.transformer.Transformer

/** Refreshes the expected value alone: the rest of a polled machine is left as the screen first rendered it. */
internal class CollectiblesUpdateExpectedValueTransformer(
    private val machine: CollectibleGachaMachine,
) : Transformer<CollectiblesMachineDetailsUM> {

    override fun transform(prevState: CollectiblesMachineDetailsUM): CollectiblesMachineDetailsUM {
        if (prevState !is CollectiblesMachineDetailsUM.Content) return prevState

        return prevState.copy(footer = prevState.footer.copy(expectedValue = machine.formatExpectedValue()))
    }
}

internal fun CollectibleGachaMachine.formatExpectedValue(): TextReference = stringReference(
    expectedValue.format {
        fiat(
            fiatCurrencyCode = AppCurrency.Default.code,
            fiatCurrencySymbol = AppCurrency.Default.symbol,
        ).optionalDecimals()
    },
)