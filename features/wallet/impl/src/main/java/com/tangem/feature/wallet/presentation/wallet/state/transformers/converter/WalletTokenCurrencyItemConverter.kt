package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.badge.*
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.feature.wallet.child.wallet.model.intents.WalletContentClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.addIf
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

internal class WalletTokenCurrencyItemConverter(
    private val appCurrency: AppCurrency,
    private val accountId: AccountId,
    private val shouldShowPromo: Boolean,
    private val yieldModuleApyMap: Map<String, BigDecimal>,
    private val clickIntents: WalletContentClickIntents,
    stakingAvailabilityMap: Map<CryptoCurrency, StakingAvailability>,
) : Converter<CryptoCurrencyStatus, TangemTokenRowUM> {

    private val currencyToIconStateConverter = CryptoCurrencyToIconStateConverter()
    private val earnApyConverter = EarnApyConverter(
        yieldModuleApyMap = yieldModuleApyMap,
        stakingApyMap = stakingAvailabilityMap,
    )

    override fun convert(value: CryptoCurrencyStatus): TangemTokenRowUM {
        val earnApyInfo = earnApyConverter.convert(value)

        return TangemTokenRowUM.Content(
            id = value.currency.id.value,
            headIconUM = TangemIconUM.Currency(
                currencyIconState = currencyToIconStateConverter.convert(value),
            ),
            titleUM = toCurrencyRowTitle(value, earnApyInfo),
            subtitleUM = toCurrencyRowSubtitle(value),
            topEndContentUM = toCurrencyRowTopEnd(value),
            bottomEndContentUM = toCurrencyRowBottomEnd(value),
            promoBannerUM = toPromoBannerUM(
                accountId,
                value,
                earnApyInfo.takeIf { shouldShowPromo },
            ),
            onItemClick = when (value.value) {
                CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.MissedDerivation,
                -> null
                else -> {
                    {
                        clickIntents.onTokenItemClick(accountId, value)
                    }
                }
            },
            onItemLongClick = when (value.value) {
                CryptoCurrencyStatus.Loading -> null
                else -> {
                    { offset, tokenRowUM ->
                        clickIntents.onTokenItemLongClickV2(
                            accountId = accountId,
                            cryptoCurrencyStatus = value,
                            offset = offset,
                            tokenRowUM = tokenRowUM,
                        )
                    }
                }
            },
        )
    }

    private fun toCurrencyRowTitle(
        currencyStatus: CryptoCurrencyStatus,
        earnApyInfo: EarnApyConverter.EarnApyInfo?,
    ): TangemTokenRowUM.TitleUM = when (val value = currencyStatus.value) {
        is CryptoCurrencyStatus.Loading,
        is CryptoCurrencyStatus.MissedDerivation,
        is CryptoCurrencyStatus.Unreachable,
        is CryptoCurrencyStatus.NoAmount,
        -> {
            TangemTokenRowUM.TitleUM.Content(
                text = stringReference(currencyStatus.currency.name),
            )
        }
        is CryptoCurrencyStatus.Loaded,
        is CryptoCurrencyStatus.Custom,
        is CryptoCurrencyStatus.NoQuote,
        is CryptoCurrencyStatus.NoAccount,
        -> {
            TangemTokenRowUM.TitleUM.Content(
                text = stringReference(currencyStatus.currency.name),
                hasPending = value.hasCurrentNetworkTransactions,
                badge = if (earnApyInfo != null && earnApyInfo.text != null) {
                    TangemBadgeUM(
                        type = TangemBadgeType.Solid,
                        color = when {
                            earnApyInfo.isActive -> TangemBadgeColor.Blue
                            else -> TangemBadgeColor.Gray
                        },
                        shape = TangemBadgeShape.Rounded,
                        size = TangemBadgeSize.X4,
                        text = earnApyInfo.text,
                    )
                } else {
                    null
                },
            )
        }
    }

    private fun toCurrencyRowSubtitle(currencyStatus: CryptoCurrencyStatus): TangemTokenRowUM.SubtitleUM {
        return when (currencyStatus.value) {
            is CryptoCurrencyStatus.Loading -> TangemTokenRowUM.SubtitleUM.Loading
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> TangemTokenRowUM.SubtitleUM.Content(
                text = stringReference(
                    currencyStatus.value.fiatRate.format {
                        fiat(fiatCurrencyCode = appCurrency.code, fiatCurrencySymbol = appCurrency.symbol)
                    },
                ),
                priceChangeUM = PriceChangeState.Content(
                    type = PriceChangeType.fromBigDecimal(currencyStatus.value.priceChange.orZero()),
                    valueInPercent = currencyStatus.value.priceChange.format { percent() },
                ),
                isFlickering = currencyStatus.value.isFlickering(),
            )
            is CryptoCurrencyStatus.MissedDerivation,
            is CryptoCurrencyStatus.Unreachable,
            is CryptoCurrencyStatus.NoAmount,
            -> TangemTokenRowUM.SubtitleUM.Empty
        }
    }

    private fun toCurrencyRowTopEnd(currencyStatus: CryptoCurrencyStatus): TangemTokenRowUM.EndContentUM {
        val yieldSupply = currencyStatus.value.yieldSupplyStatus
        return when (currencyStatus.value) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> {
                TangemTokenRowUM.EndContentUM.Content(
                    text = currencyStatus.getTotalFiatAmount().formatStyled {
                        fiat(
                            fiatCurrencyCode = appCurrency.code,
                            fiatCurrencySymbol = appCurrency.symbol,
                            spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                        )
                    },
                    isFlickering = currencyStatus.value.isFlickering(),
                    startIcons = buildList {
                        addIf(
                            element = TangemIconUM.Icon(
                                iconRes = R.drawable.ic_attention_default_24,
                                tintReference = { TangemTheme.colors2.graphic.status.attention },
                            ),
                            condition = yieldSupply?.isActive == true && !yieldSupply.isAllowedToSpend,
                        )
                        addIf(
                            element = TangemIconUM.Icon(
                                iconRes = R.drawable.ic_error_sync_default_24,
                                tintReference = { TangemTheme.colors2.graphic.neutral.tertiary },
                            ),
                            condition = currencyStatus.value.sources.total == StatusSource.ONLY_CACHE,
                        )
                    }.toImmutableList(),
                )
            }
            is CryptoCurrencyStatus.Loading -> TangemTokenRowUM.EndContentUM.Loading
            is CryptoCurrencyStatus.MissedDerivation -> TangemTokenRowUM.EndContentUM.Content(
                text = stringReference(StringsSigns.DASH_SIGN),
            )
            is CryptoCurrencyStatus.Unreachable -> TangemTokenRowUM.EndContentUM.Content(
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
            is CryptoCurrencyStatus.NoAmount,
            -> TangemTokenRowUM.EndContentUM.Empty
        }
    }

    private fun toCurrencyRowBottomEnd(currencyStatus: CryptoCurrencyStatus): TangemTokenRowUM.EndContentUM {
        return when (currencyStatus.value) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> TangemTokenRowUM.EndContentUM.Content(
                text = stringReference(
                    currencyStatus.getTotalCryptoAmount().format {
                        crypto(cryptoCurrency = currencyStatus.currency)
                    },
                ),
                isFlickering = currencyStatus.value.isFlickering(),
            )
            is CryptoCurrencyStatus.Loading -> TangemTokenRowUM.EndContentUM.Loading
            is CryptoCurrencyStatus.MissedDerivation -> TangemTokenRowUM.EndContentUM.Content(
                text = styledResourceReference(
                    id = R.string.common_no_address,
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.status.attention) },
                ),
                endIcons = persistentListOf(
                    TangemIconUM.Icon(
                        iconRes = R.drawable.ic_attention_default_24,
                        tintReference = { TangemTheme.colors2.graphic.status.attention },
                    ),
                ),
            )
            is CryptoCurrencyStatus.Unreachable -> TangemTokenRowUM.EndContentUM.Content(
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
            is CryptoCurrencyStatus.NoAmount,
            -> TangemTokenRowUM.EndContentUM.Empty
        }
    }

    private fun toPromoBannerUM(
        accountId: AccountId,
        currencyStatus: CryptoCurrencyStatus,
        earnApyInfo: EarnApyConverter.EarnApyInfo?,
    ): TangemTokenRowUM.PromoBannerUM {
        val currency = currencyStatus.currency
        val isTokenCurrency = currency is CryptoCurrency.Token
        val isCurrencyStatusLoaded = currencyStatus.value is CryptoCurrencyStatus.Loaded
        val isApyInfoNotNull = earnApyInfo != null && earnApyInfo.apy != null

        if (!isTokenCurrency || !isCurrencyStatusLoaded || !isApyInfoNotNull) {
            return TangemTokenRowUM.PromoBannerUM.Empty
        }

        return TangemTokenRowUM.PromoBannerUM.Content(
            title = resourceReference(
                R.string.yield_module_main_screen_promo_banner_message,
                wrappedList(earnApyInfo.apy),
            ),
            iconRes = R.drawable.ic_yield_mode_mini_12,
            type = TangemTokenRowUM.PromoBannerUM.Content.Type.Yield,
            onPromoBannerClick = {
                clickIntents.onYieldPromoClicked(accountId, currencyStatus, earnApyInfo.apy)
            },
            onCloseClick = clickIntents::onYieldPromoCloseClick,
            onPromoShown = {
                clickIntents.onYieldPromoShown(currency)
            },
        )
    }

    private fun CryptoCurrencyStatus.Value.isFlickering(): Boolean = sources.total == StatusSource.CACHE
}