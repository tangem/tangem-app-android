package com.tangem.features.foryou.impl.model.converter.earnOpportunities

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.common.ui.tokens.TokenItemStateConverter.Companion.isFlickering
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM.SubtitleUM.Content
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.percent
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.foryou.impl.entity.ForYouEarnOpportunitiesType
import com.tangem.features.foryou.impl.model.converter.EarnApyInfo
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

/**
 * Maps one earn-eligible portfolio holding to a token row: network subtitle, projected yearly earn
 * (`fiat balance * rate`) as the top end and the rate itself as the styled bottom end.
 *
 * Non-resolved statuses degrade the same way as in the portfolio review: loading → skeleton row,
 * no-quote / no-address / unreachable → dashes, stale cache → error-sync icon on both ends.
 */
internal class ForYouEarnOpportunitiesTokenRowConverter(
    private val appCurrency: AppCurrency,
    private val userWalletId: UserWalletId?,
    private val onTokenClick: (UserWalletId?, CryptoCurrency, ForYouEarnOpportunitiesType) -> Unit,
) : Converter<Pair<CryptoCurrencyStatus, EarnApyInfo>, TangemTokenRowUM> {

    private val iconConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: Pair<CryptoCurrencyStatus, EarnApyInfo>): TangemTokenRowUM {
        val (cryptoCurrencyStatus, earnApyInfo) = value
        if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) {
            return TangemTokenRowUM.Loading(id = cryptoCurrencyStatus.currency.id.value)
        }

        val possibleEarnAmount = cryptoCurrencyStatus.value.fiatAmount.orZero().multiply(earnApyInfo.apy.orZero())

        return TangemTokenRowUM.Content(
            id = cryptoCurrencyStatus.currency.id.value,
            headIconUM = TangemIconUM.Currency(iconConverter.convert(cryptoCurrencyStatus)),
            titleUM = TangemTokenRowUM.TitleUM.Content(text = stringReference(cryptoCurrencyStatus.currency.name)),
            subtitleUM = Content(
                text = resourceReference(
                    R.string.wallet_network_group_title,
                    wrappedList(cryptoCurrencyStatus.currency.network.name),
                ),
            ),
            topEndContentUM = toRowTopEnd(cryptoCurrencyStatus, possibleEarnAmount),
            bottomEndContentUM = toRowBottomEnd(cryptoCurrencyStatus, earnApyInfo.apy.orZero()),
            onItemClick = {
                onTokenClick(
                    userWalletId,
                    cryptoCurrencyStatus.currency,
                    earnApyInfo.type,
                )
            },
            onItemLongClick = null,
        )
    }

    /** Top-end: fiat total for resolved states, dash / unreachable treatment otherwise. */
    private fun toRowTopEnd(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        possibleEarnAmount: BigDecimal,
    ): TangemTokenRowUM.EndContentUM = when (cryptoCurrencyStatus.value) {
        CryptoCurrencyStatus.Loading -> TangemTokenRowUM.EndContentUM.Loading
        is CryptoCurrencyStatus.Custom,
        is CryptoCurrencyStatus.Loaded,
        is CryptoCurrencyStatus.NoAccount,
        -> {
            val possibleEarn = possibleEarnAmount.format {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                )
            }

            TangemTokenRowUM.EndContentUM.Content(
                text = combinedReference(
                    stringReference(StringsSigns.PLUS),
                    resourceReference(
                        R.string.for_you_earn_per_year,
                        wrappedList(possibleEarn),
                    ),
                ),
                isFlickering = cryptoCurrencyStatus.value.isFlickering(),
                startIcons = buildList {
                    if (cryptoCurrencyStatus.value.sources.total == StatusSource.ONLY_CACHE) {
                        add(
                            TangemIconUM.Icon(
                                iconRes = R.drawable.ic_error_sync_default_24,
                                tintReference = { TangemTheme.colors3.icon.tertiary },
                            ),
                        )
                    }
                }.toImmutableList(),
            )
        }
        is CryptoCurrencyStatus.NoQuote,
        is CryptoCurrencyStatus.MissedDerivation,
        is CryptoCurrencyStatus.NoAmount,
        is CryptoCurrencyStatus.Unreachable,
        -> TangemTokenRowUM.EndContentUM.Content(text = stringReference(StringsSigns.DASH_SIGN))
    }

    /** Bottom-end: percentage share for resolved states, no-address / unreachable treatment otherwise. */
    private fun toRowBottomEnd(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        earnRate: BigDecimal,
    ): TangemTokenRowUM.EndContentUM = when (cryptoCurrencyStatus.value) {
        CryptoCurrencyStatus.Loading -> TangemTokenRowUM.EndContentUM.Loading
        is CryptoCurrencyStatus.Custom,
        is CryptoCurrencyStatus.Loaded,
        is CryptoCurrencyStatus.NoAccount,
        -> {
            TangemTokenRowUM.EndContentUM.Content(
                text = styledStringReference(
                    value = earnRate.format { percent() },
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.status.success) },
                ),
                isFlickering = cryptoCurrencyStatus.value.isFlickering(),
                startIcons = buildList {
                    if (cryptoCurrencyStatus.value.sources.total == StatusSource.ONLY_CACHE) {
                        add(
                            TangemIconUM.Icon(
                                iconRes = R.drawable.ic_error_sync_default_24,
                                tintReference = { TangemTheme.colors3.icon.tertiary },
                            ),
                        )
                    }
                }.toImmutableList(),
            )
        }
        is CryptoCurrencyStatus.NoQuote,
        is CryptoCurrencyStatus.MissedDerivation,
        is CryptoCurrencyStatus.NoAmount,
        is CryptoCurrencyStatus.Unreachable,
        -> TangemTokenRowUM.EndContentUM.Content(text = stringReference(StringsSigns.DASH_SIGN))
    }
}