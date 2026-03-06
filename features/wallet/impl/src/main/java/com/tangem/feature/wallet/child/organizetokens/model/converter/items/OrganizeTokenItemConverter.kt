package com.tangem.feature.wallet.child.organizetokens.model.converter.items

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.internal.TangemRowTailUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.cryptoStyled
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.account.status.model.AccountCryptoCurrencyStatus
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import com.tangem.feature.wallet.child.organizetokens.model.common.getTokenItemId
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.converter.Converter

internal class OrganizeTokenItemConverter(
    private val appCurrency: AppCurrency,
) : Converter<AccountCryptoCurrencyStatus, OrganizeRowItemUM.Token> {

    private val iconStateConverter by lazy(LazyThreadSafetyMode.NONE) {
        CryptoCurrencyToIconStateConverter()
    }

    override fun convert(value: AccountCryptoCurrencyStatus): OrganizeRowItemUM.Token {
        val (account, currencyStatus) = value
        val currency = currencyStatus.currency

        return OrganizeRowItemUM.Token(
            tokenRowUM = TangemTokenRowUM.Actionable(
                id = getTokenItemId(currency.id),
                headIconUM = TangemIconUM.Currency(iconStateConverter.convert(currencyStatus)),
                titleUM = TangemTokenRowUM.TitleUM.Content(
                    text = stringReference(currency.name),
                ),
                subtitleUM = TangemTokenRowUM.SubtitleUM.Empty,
                topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                    text = currencyStatus.getTotalFiatAmount().formatStyled {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                        )
                    },
                ),
                bottomEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                    text = currencyStatus.getTotalCryptoAmount().formatStyled {
                        cryptoStyled(
                            cryptoCurrency = currency,
                            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                        )
                    },
                ),
                tailUM = TangemRowTailUM.Draggable(R.drawable.ic_drag_24),
                onItemClick = null,
                onItemLongClick = null,
            ),
            groupId = currency.network.id.toString(),
            accountId = account.accountId.value,
        )
    }
}