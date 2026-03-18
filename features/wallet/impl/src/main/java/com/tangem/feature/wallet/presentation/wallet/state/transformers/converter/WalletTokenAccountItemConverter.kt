package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.quote.PriceChange
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class WalletTokenAccountItemConverter(
    private val appCurrency: AppCurrency,
    private val expandedAccounts: Set<AccountId>,
    private val onAccountCollapseClick: (account: Account) -> Unit,
    private val onAccountExpandClick: (account: Account) -> Unit,
) : Converter<AccountStatus.CryptoPortfolio, TangemTokenRowUM> {
    override fun convert(value: AccountStatus.CryptoPortfolio): TangemTokenRowUM {
        val account = value.account
        val isExpanded = expandedAccounts.contains(account.accountId)

        return TangemTokenRowUM.Content(
            id = account.accountId.value,
            headIconUM = TangemIconUM.Currency(
                currencyIconState = AccountIconItemStateConverter(size = AccountIconSize.ExtraSmall).convert(account),
            ),
            titleUM = TangemTokenRowUM.TitleUM.Content(
                text = account.accountName.toUM().value,
            ),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                text = pluralReference(
                    R.plurals.common_tokens_count,
                    count = account.tokensCount,
                    formatArgs = wrappedList(account.tokensCount),
                ),
            ),
            topEndContentUM = getTopEndContent(value.tokenList.totalFiatBalance),
            bottomEndContentUM = getBottomEndContent(
                value.tokenList.totalFiatBalance,
                value.priceChangeLce.getOrNull(),
            ),
            onItemClick = {
                if (isExpanded) {
                    onAccountCollapseClick(account)
                } else {
                    onAccountExpandClick(account)
                }
            },
            onItemLongClick = null,
        )
    }

    private fun getTopEndContent(accountBalance: TotalFiatBalance): TangemTokenRowUM.EndContentUM {
        return when (accountBalance) {
            TotalFiatBalance.Failed -> TangemTokenRowUM.EndContentUM.Content(
                text = stringReference(StringsSigns.DASH_SIGN),
            )
            is TotalFiatBalance.Loaded -> TangemTokenRowUM.EndContentUM.Content(
                text = accountBalance.amount.formatStyled {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                        spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                    )
                },
            )
            TotalFiatBalance.Loading -> TangemTokenRowUM.EndContentUM.Loading
        }
    }

    private fun getBottomEndContent(
        accountBalance: TotalFiatBalance,
        priceChange: PriceChange?,
    ): TangemTokenRowUM.EndContentUM {
        return when (accountBalance) {
            TotalFiatBalance.Failed -> TangemTokenRowUM.EndContentUM.Content(
                text = styledResourceReference(
                    id = R.string.common_unreachable,
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.status.attention) },
                ),
                endIcons = persistentListOf(
                    TangemIconUM.Icon(
                        iconRes = R.drawable.ic_attention_default_24,
                        tintReference = { TangemTheme.colors2.graphic.status.attention },
                    ),
                ),
            )
            is TotalFiatBalance.Loaded -> if (priceChange != null) {
                val priceChangeType = PriceChangeType.fromBigDecimal(priceChange.value)

                TangemTokenRowUM.EndContentUM.Content(
                    text = stringReference(
                        priceChange.value.format {
                            fiat(
                                fiatCurrencyCode = appCurrency.code,
                                fiatCurrencySymbol = appCurrency.symbol,
                            )
                        },
                    ),
                    priceChangeUM = PriceChangeState.Content(
                        type = priceChangeType,
                        valueInPercent = priceChange.value.format { percent() },
                    ),
                )
            } else {
                TangemTokenRowUM.EndContentUM.Empty
            }
            TotalFiatBalance.Loading -> TangemTokenRowUM.EndContentUM.Loading
        }
    }
}