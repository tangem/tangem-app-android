package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.main.model.CollectiblesBalance
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.converter.Converter

internal class CollectiblesBalanceConverter : Converter<CollectiblesBalance, CollectiblesMainUM.BalanceUM> {

    override fun convert(value: CollectiblesBalance): CollectiblesMainUM.BalanceUM {
        return CollectiblesMainUM.BalanceUM(
            fiatStyled = value.fiat.formatStyled {
                fiat(
                    fiatCurrencyCode = FIAT_CODE,
                    fiatCurrencySymbol = FIAT_SYMBOL,
                    spanStyleReference = { TangemTheme.typography3.heading.medium.toSpanStyle() },
                )
            },
            fiatPlain = stringReference(
                value.fiat.format {
                    fiat(fiatCurrencyCode = FIAT_CODE, fiatCurrencySymbol = FIAT_SYMBOL).defaultAmount()
                },
            ),
            crypto = stringReference(value.crypto),
        )
    }

    private companion object {

        const val FIAT_CODE = "USD"
        const val FIAT_SYMBOL = "$"
    }
}