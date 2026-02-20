package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.text.SpanStyle
import com.tangem.common.getTotalCryptoAmount
import com.tangem.common.getTotalFiatAmount
import com.tangem.common.ui.account.AccountIconItemStateConverter
import com.tangem.common.ui.account.toUM
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds.badge.*
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM.EndContentUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.format.bigdecimal.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.addIf
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

@Suppress("LargeClass", "LongParameterList")
internal class WalletTokensListUMTransformer(
    private val appCurrency: AppCurrency,
    private val selectedWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
    private val yieldModuleApyMap: Map<String, BigDecimal>,
    private val isAccountsModeEnabled: Boolean,
    private val expandedAccounts: Set<AccountId>,
    stakingAvailabilityMap: Map<CryptoCurrency, StakingAvailability>,
    shouldShowMainPromo: Boolean,
) : Converter<AccountStatusList, WalletTokensListUM> {

    private val yieldSupplyPromoBannerConverter = YieldSupplyPromoBannerConverter(
        yieldModuleApyMap,
        shouldShowMainPromo,
    )
    private val currencyToIconStateConverter = CryptoCurrencyToIconStateConverter()
    private val earnApyConverter = EarnApyConverter(
        yieldModuleApyMap = yieldModuleApyMap,
        stakingApyMap = stakingAvailabilityMap,
    )

    override fun convert(value: AccountStatusList): WalletTokensListUM {
        val promoCryptoCurrency = yieldSupplyPromoBannerConverter.convert2(value = value)
        return if (value.accountStatuses.isEmpty()) {
            WalletTokensListUM.Empty
        } else {
            val isCollapsable = value.accountStatuses.count {
                it is AccountStatus.CryptoPortfolio && it.account.tokensCount > 0
            } > 1

            val tokenListUM = value.accountStatuses
                .filterIsInstance<AccountStatus.CryptoPortfolio>()
                .asSequence()
                .flatMap { accountStatus ->
                    if (isAccountsModeEnabled) {
                        val isExpanded = expandedAccounts.contains(accountStatus.account.accountId)
                        sequenceOf(
                            TokensListItemUM2.Portfolio(
                                tokenRowUM = toAccountRow(accountStatus, isExpanded),
                                isExpanded = isExpanded || !isCollapsable,
                                isCollapsable = isCollapsable,
                                tokenList = getTokenListItems(
                                    accountStatus.tokenList,
                                    promoCryptoCurrency,
                                ).toPersistentList(),
                            ),
                        )
                    } else {
                        getTokenListItems(accountStatus.tokenList, promoCryptoCurrency)
                    }
                }.toPersistentList()

            WalletTokensListUM.Content(
                tokenList = tokenListUM,
                organizeButtonUM = getOrganizeButtonUM(value),
            )
        }
    }

    private fun getTokenListItems(
        tokenList: TokenList,
        promoCryptoCurrency: CryptoCurrencyStatus?,
    ): Sequence<TokensListItemUM2> {
        return when (tokenList) {
            TokenList.Empty -> emptySequence()
            is TokenList.GroupedByNetwork -> {
                tokenList.groups.asSequence().flatMap { (network, currencies) ->
                    buildList {
                        add(
                            TokensListItemUM2.GroupTitle(
                                tokenRowUM = toGroupRow(network),
                            ),
                        )
                        addAll(
                            currencies.asSequence().map { currencyStatus ->
                                val shouldShowPromo = promoCryptoCurrency?.currency?.id == currencyStatus.currency.id
                                TokensListItemUM2.Token(
                                    tokenRowUM = toCurrencyRow(
                                        currencyStatus = currencyStatus,
                                        shouldShowPromo = shouldShowPromo,
                                    ),
                                )
                            }.toList(),
                        )
                    }
                }
            }
            is TokenList.Ungrouped -> {
                tokenList.currencies.asSequence().map { currencyStatus ->
                    TokensListItemUM2.Token(
                        toCurrencyRow(
                            currencyStatus = currencyStatus,
                            shouldShowPromo = promoCryptoCurrency?.currency?.id == currencyStatus.currency.id,
                        ),
                    )
                }
            }
        }
    }

    private fun toAccountRow(accountStatus: AccountStatus.CryptoPortfolio, isExpanded: Boolean): TangemTokenRowUM {
        val account = accountStatus.account

        val (topEndContent, bottomEndContent) = when (val accountBalance = accountStatus.tokenList.totalFiatBalance) {
            TotalFiatBalance.Failed -> toFailedAccountRow()
            is TotalFiatBalance.Loaded -> toLoadedAccountRow(accountStatus, accountBalance)
            TotalFiatBalance.Loading -> EndContentUM.Loading to EndContentUM.Loading
        }

        return TangemTokenRowUM.Content(
            id = accountStatus.account.accountId.value,
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
            topEndContentUM = topEndContent,
            bottomEndContentUM = bottomEndContent,
            onItemClick = {
                if (isExpanded) {
                    clickIntents.onAccountCollapseClick(account)
                } else {
                    clickIntents.onAccountExpandClick(account)
                }
            },
            onItemLongClick = null,
        )
    }

    private fun toFailedAccountRow(): Pair<EndContentUM.Content, EndContentUM.Content> {
        return EndContentUM.Content(
            text = stringReference(StringsSigns.DASH_SIGN),
        ) to EndContentUM.Content(
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
    }

    private fun toLoadedAccountRow(
        accountStatus: AccountStatus.CryptoPortfolio,
        accountBalance: TotalFiatBalance.Loaded,
    ): Pair<EndContentUM.Content, EndContentUM> {
        val priceChange = accountStatus.priceChangeLce.getOrNull()

        return EndContentUM.Content(
            text = accountBalance.amount.formatStyled {
                fiat(
                    fiatCurrencyCode = appCurrency.code,
                    fiatCurrencySymbol = appCurrency.symbol,
                    spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                )
            },
        ) to if (priceChange != null) {
            val priceChangeType = PriceChangeType.fromBigDecimal(priceChange.value)

            EndContentUM.Content(
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
            EndContentUM.Empty
        }
    }

    private fun toGroupRow(network: Network): TangemHeaderRowUM {
        return TangemHeaderRowUM(
            id = network.hashCode().toString(),
            title = resourceReference(
                id = R.string.wallet_network_group_title,
                formatArgs = wrappedList(network.name),
            ),
        )
    }

    private fun toCurrencyRow(currencyStatus: CryptoCurrencyStatus, shouldShowPromo: Boolean): TangemTokenRowUM {
        val earnApyInfo = earnApyConverter.convert(currencyStatus)

        return TangemTokenRowUM.Content(
            id = currencyStatus.currency.id.value,
            headIconUM = TangemIconUM.Currency(
                currencyIconState = currencyToIconStateConverter.convert(currencyStatus),
            ),
            titleUM = toCurrencyRowTitle(currencyStatus, earnApyInfo),
            subtitleUM = toCurrencyRowSubtitle(currencyStatus),
            topEndContentUM = toCurrencyRowTopEnd(currencyStatus),
            bottomEndContentUM = toCurrencyRowBottomEnd(currencyStatus),
            promoBannerUM = toPromoBannerUM(
                currencyStatus,
                earnApyInfo.takeIf { shouldShowPromo },
            ),
            onItemClick = when (currencyStatus.value) {
                CryptoCurrencyStatus.Loading,
                is CryptoCurrencyStatus.MissedDerivation,
                -> null
                else -> {
                    {
                        clickIntents.onTokenItemClick(selectedWallet.walletId, currencyStatus)
                    }
                }
            },
            onItemLongClick = when (currencyStatus.value) {
                CryptoCurrencyStatus.Loading -> null
                else -> {
                    {
                        clickIntents.onTokenItemLongClick(selectedWallet.walletId, currencyStatus)
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
                        onClick = if (earnApyInfo.apy != null) {
                            {
                                clickIntents.onApyLabelClick(
                                    userWalletId = selectedWallet.walletId,
                                    currencyStatus = currencyStatus,
                                    apySource = earnApyInfo.source,
                                    apy = earnApyInfo.apy,
                                )
                            }
                        } else {
                            null
                        },
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

    private fun toCurrencyRowTopEnd(currencyStatus: CryptoCurrencyStatus): EndContentUM {
        val yieldSupply = currencyStatus.value.yieldSupplyStatus
        return when (currencyStatus.value) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> {
                EndContentUM.Content(
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
            is CryptoCurrencyStatus.Loading -> EndContentUM.Loading
            is CryptoCurrencyStatus.MissedDerivation -> EndContentUM.Content(
                text = stringReference(StringsSigns.DASH_SIGN),
            )
            is CryptoCurrencyStatus.Unreachable -> EndContentUM.Content(
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
            -> EndContentUM.Empty
        }
    }

    private fun toCurrencyRowBottomEnd(currencyStatus: CryptoCurrencyStatus): EndContentUM {
        return when (currencyStatus.value) {
            is CryptoCurrencyStatus.Loaded,
            is CryptoCurrencyStatus.Custom,
            is CryptoCurrencyStatus.NoQuote,
            is CryptoCurrencyStatus.NoAccount,
            -> EndContentUM.Content(
                text = stringReference(
                    currencyStatus.getTotalCryptoAmount().format {
                        crypto(cryptoCurrency = currencyStatus.currency)
                    },
                ),
                isFlickering = currencyStatus.value.isFlickering(),
            )
            is CryptoCurrencyStatus.Loading -> EndContentUM.Loading
            is CryptoCurrencyStatus.MissedDerivation -> EndContentUM.Content(
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
            is CryptoCurrencyStatus.Unreachable -> EndContentUM.Content(
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
            -> EndContentUM.Empty
        }
    }

    private fun toPromoBannerUM(
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
            onPromoBannerClick = {
                clickIntents.onYieldPromoClicked(currency)
                clickIntents.onApyLabelClick(
                    userWalletId = selectedWallet.walletId,
                    currencyStatus = currencyStatus,
                    apySource = earnApyInfo.source,
                    apy = earnApyInfo.apy,
                )
            },
            onCloseClick = clickIntents::onYieldPromoCloseClick,
            onPromoShown = {
                clickIntents.onYieldPromoShown(currency)
            },
        )
    }

    private fun getOrganizeButtonUM(accountList: AccountStatusList): TangemButtonUM? {
        return if (accountList.flattenCurrencies().size > 1 && !isSingleCurrencyWalletWithToken()) {
            TangemButtonUM(
                text = resourceReference(R.string.organize_tokens_title),
                isEnabled = accountList.totalFiatBalance !is TotalFiatBalance.Loading,
                size = TangemButtonSize.X9,
                shape = TangemButtonShape.Rounded,
                type = TangemButtonType.PrimaryInverse,
                iconRes = R.drawable.ic_filter_default_24,
                onClick = clickIntents::onOrganizeTokensClick,
            )
        } else {
            null
        }
    }

    private fun CryptoCurrencyStatus.Value.isFlickering(): Boolean = sources.total == StatusSource.CACHE

    private fun isSingleCurrencyWalletWithToken(): Boolean {
        return selectedWallet is UserWallet.Cold &&
            selectedWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
    }
}