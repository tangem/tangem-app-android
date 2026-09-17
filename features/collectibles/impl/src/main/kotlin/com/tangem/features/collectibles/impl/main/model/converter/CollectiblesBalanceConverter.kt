package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.defaultAmount
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.StringsSigns.DASH_SIGN
import com.tangem.utils.converter.Converter

/** A vault that has not been read yet shows a dash: a zero would claim it is empty. */
internal class CollectiblesBalanceConverter :
    Converter<CollectiblesBalanceConverter.Input, CollectiblesMainUM.BalanceUM> {

    override fun convert(value: Input): CollectiblesMainUM.BalanceUM {
        return CollectiblesMainUM.BalanceUM(
            fiatStyled = value.status.fiatBalance.formatStyled {
                fiat(
                    fiatCurrencyCode = value.appCurrency.code,
                    fiatCurrencySymbol = value.appCurrency.symbol,
                    spanStyleReference = { TangemTheme.typography3.heading.medium.toSpanStyle() },
                )
            },
            fiatPlain = stringReference(
                value.status.fiatBalance.format {
                    fiat(
                        fiatCurrencyCode = value.appCurrency.code,
                        fiatCurrencySymbol = value.appCurrency.symbol,
                    ).defaultAmount()
                },
            ),
            crypto = stringReference(
                value.status.balance?.let { balance ->
                    balance.usdcAmount.format { crypto(cryptoCurrency = balance.usdcToken) }
                } ?: DASH_SIGN,
            ),
        )
    }

    data class Input(val status: CollectiblesAccountStatusValue, val appCurrency: AppCurrency)
}