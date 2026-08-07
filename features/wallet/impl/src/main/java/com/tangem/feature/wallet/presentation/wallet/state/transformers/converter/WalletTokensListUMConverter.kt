package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.formatStyled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
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
import com.tangem.feature.wallet.presentation.wallet.state.utils.isSingleCurrency
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class WalletTokensListUMConverter(
    private val appCurrency: AppCurrency,
    private val selectedWallet: UserWallet,
    private val clickIntents: WalletClickIntents,
    private val yieldModuleApyMap: Map<String, BigDecimal>,
    private val isAccountsModeEnabled: Boolean,
    private val expandedAccounts: Set<AccountId>,
    private val stakingAvailabilityMap: Map<CryptoCurrency, StakingAvailability>,
    private val isPolymarketEnabled: Boolean,
    shouldShowMainPromo: Boolean,
) : Converter<AccountStatusList, WalletTokensListUM> {

    private val accountRowConverter by lazy(LazyThreadSafetyMode.NONE) {
        WalletTokenAccountItemConverter(
            appCurrency = appCurrency,
            expandedAccounts = expandedAccounts,
            onAccountCollapseClick = clickIntents::onAccountCollapseClick,
            onAccountExpandClick = clickIntents::onAccountExpandClick,
        )
    }

    private val yieldSupplyPromoBannerConverter by lazy(LazyThreadSafetyMode.NONE) {
        YieldSupplyPromoBannerConverter(
            yieldModuleApyMap,
            shouldShowMainPromo,
        )
    }

    private fun currencyRowConverter(
        accountId: AccountId,
        shouldShowPromo: Boolean,
    ): WalletTokenCurrencyItemConverter {
        return WalletTokenCurrencyItemConverter(
            appCurrency = appCurrency,
            accountId = accountId,
            shouldShowPromo = shouldShowPromo,
            yieldModuleApyMap = yieldModuleApyMap,
            stakingAvailabilityMap = stakingAvailabilityMap,
            clickIntents = clickIntents,
        )
    }

    override fun convert(value: AccountStatusList): WalletTokensListUM {
        val promoCryptoCurrency = yieldSupplyPromoBannerConverter.convert2(value = value)
        return if (value.flattenCurrencies().isEmpty()) {
            WalletTokensListUM.Empty(
                onEmptyClick = { clickIntents.onManageTokensClick(value.mainAccount.accountId) },
            )
        } else {
            val tokenListUM = value.accountStatuses
                .filterIsInstance<AccountStatus.CryptoPortfolio>()
                .asSequence()
                .flatMap { accountStatus ->
                    if (isAccountsModeEnabled) {
                        sequenceOf(
                            TokensListItemUM2.Portfolio(
                                tokenRowUM = accountRowConverter.convert(accountStatus),
                                isExpanded = expandedAccounts.contains(accountStatus.account.accountId),
                                isCollapsable = true,
                                onEmptyClick = { clickIntents.onManageTokensClick(accountStatus.account.accountId) },
                                tokenList = getTokenListItems(
                                    accountStatus,
                                    promoCryptoCurrency,
                                ).toPersistentList(),
                            ),
                        )
                    } else {
                        getTokenListItems(accountStatus, promoCryptoCurrency)
                    }
                }.toPersistentList()

            val tokenListWithEntryPoints = if (isPolymarketEnabled) {
                tokenListUM.add(index = 0, element = TokensListItemUM2.Prediction(tokenRowUM = getPredictionRow()))
            } else {
                tokenListUM
            }

            WalletTokensListUM.Content(
                tokenList = tokenListWithEntryPoints,
                organizeButtonUM = getOrganizeButtonUM(value),
            )
        }
    }

    private fun getPredictionRow(): TangemTokenRowUM {
        return TangemTokenRowUM.Content(
            id = PREDICTION_ROW_ID,
            // TODO([REDACTED_TASK_KEY]): replace the placeholder icon with the final Polymarket account asset
            headIconUM = TangemIconUM.Currency(
                currencyIconState = CurrencyIconState.CryptoPortfolio.Icon(
                    resId = R.drawable.ic_analytics_up_24,
                    color = PREDICTION_ICON_COLOR,
                    isGrayscale = false,
                    size = AccountIconSize.ExtraSmall,
                ),
            ),
            titleUM = TangemTokenRowUM.TitleUM.Content(
                text = resourceReference(R.string.prediction_account_title),
            ),
            subtitleUM = TangemTokenRowUM.SubtitleUM.Content(
                text = resourceReference(R.string.prediction_account_subtitle),
            ),
            topEndContentUM = TangemTokenRowUM.EndContentUM.Content(
                text = BigDecimal.ZERO.formatStyled {
                    fiat(
                        fiatCurrencyCode = appCurrency.code,
                        fiatCurrencySymbol = appCurrency.symbol,
                        spanStyleReference = { SpanStyle(color = TangemTheme.colors2.text.neutral.secondary) },
                    )
                },
            ),
            bottomEndContentUM = TangemTokenRowUM.EndContentUM.Empty,
            onItemClick = { clickIntents.onPredictionAccountClick(selectedWallet.walletId) },
            onItemLongClick = null,
        )
    }

    private fun getTokenListItems(
        accountStatus: AccountStatus.CryptoPortfolio,
        promoCryptoCurrency: CryptoCurrencyStatus?,
    ): Sequence<TokensListItemUM2> {
        return when (val tokenList = accountStatus.tokenList) {
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
                                    tokenRowUM = currencyRowConverter(
                                        accountStatus.accountId,
                                        shouldShowPromo,
                                    ).convert(currencyStatus),
                                )
                            }.toList(),
                        )
                    }
                }
            }
            is TokenList.Ungrouped -> {
                tokenList.currencies.asSequence().map { currencyStatus ->
                    val shouldShowPromo = promoCryptoCurrency?.currency?.id == currencyStatus.currency.id
                    TokensListItemUM2.Token(
                        tokenRowUM = currencyRowConverter(
                            accountStatus.accountId,
                            shouldShowPromo,
                        ).convert(currencyStatus),
                    )
                }
            }
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

    private fun getOrganizeButtonUM(accountList: AccountStatusList): TangemButtonUM? {
        val textRes = R.string.main_add_and_manage_tokens
        val iconRes = R.drawable.ic_filter_default_24
        return if (accountList.flattenCurrencies().isNotEmpty() && !selectedWallet.isSingleCurrency()) {
            TangemButtonUM(
                text = resourceReference(textRes),
                isEnabled = accountList.totalFiatBalance !is TotalFiatBalance.Loading,
                size = TangemButtonSize.X9,
                shape = TangemButtonShape.Rounded,
                type = TangemButtonType.PrimaryInverse,
                tangemIconUM = TangemIconUM.Icon(
                    iconRes = iconRes,
                    tintReference = {
                        if (accountList.totalFiatBalance !is TotalFiatBalance.Loading) {
                            TangemTheme.colors2.graphic.neutral.primary
                        } else {
                            TangemTheme.colors2.graphic.neutral.quaternary
                        }
                    },
                ),
                onClick = clickIntents::onOrganizeTokensClick,
            )
        } else {
            null
        }
    }

    @Suppress("MagicNumber")
    private companion object {
        const val PREDICTION_ROW_ID = "polymarket_prediction_account"
        val PREDICTION_ICON_COLOR = Color(0xFF5A5AF0)
    }
}