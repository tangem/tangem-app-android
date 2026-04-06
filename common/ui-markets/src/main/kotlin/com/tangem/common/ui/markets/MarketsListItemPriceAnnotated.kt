package com.tangem.common.ui.markets

import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.price
import com.tangem.core.ui.res.TangemTheme
import java.math.BigDecimal

/**
 * Fiat amount for markets list rows: integer part in primary style, fractional part (from locale separator) in secondary.
 */
fun BigDecimal.toMarketsListItemPriceAnnotated(appCurrencyCode: String, appCurrencySymbol: String): TextReference {
    return formatStyled {
        fiat(
            fiatCurrencyCode = appCurrencyCode,
            fiatCurrencySymbol = appCurrencySymbol,
            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
        ).price()
    }
}