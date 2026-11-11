package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.account.isShown
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.TokensListItemUM2
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTokensListUM
import com.tangem.feature.wallet.presentation.wallet.state.utils.isSingleCurrency
import com.tangem.features.earn.api.overview.EarnOverviews
import com.tangem.utils.annotations.RemoveWithToggle
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
    private val earnOverviews: EarnOverviews,
    shouldShowMainPromo: Boolean,
    private val jointPendingOperations: List<JointPendingOperation> = emptyList(),
    private val isRedesignedAccountRowsEnabled: Boolean = false,
    @RemoveWithToggle(toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED", description = "Inline true")
    private val isRedesignedTokenRowsEnabled: Boolean = false,
) : Converter<AccountStatusList, WalletTokensListUM> {

    private val jointAccountRowConverter by lazy(LazyThreadSafetyMode.NONE) {
        WalletJointAccountItemConverter(
            appCurrency = appCurrency,
            userWalletId = selectedWallet.walletId,
            onAccountClick = clickIntents::onJointAccountClick,
            onCompromisedAccountClick = clickIntents::onCompromisedJointAccountClick,
        )
    }

    private val accountRowConverter by lazy(LazyThreadSafetyMode.NONE) {
        WalletTokenAccountItemConverter(
            appCurrency = appCurrency,
            expandedAccounts = expandedAccounts,
            onAccountCollapseClick = clickIntents::onAccountCollapseClick,
            onAccountExpandClick = clickIntents::onAccountExpandClick,
            jointPendingOperations = jointPendingOperations,
        )
    }

    private val accountRowV2Converter by lazy(LazyThreadSafetyMode.NONE) {
        WalletAccountRowConverter(
            appCurrency = appCurrency,
            expandedAccounts = expandedAccounts,
            onAccountCollapseClick = clickIntents::onAccountCollapseClick,
            onAccountExpandClick = clickIntents::onAccountExpandClick,
            onManageTokensClick = clickIntents::onManageTokensClick,
            jointPendingOperations = jointPendingOperations,
        )
    }

    private val collectiblesAccountRowConverter by lazy(LazyThreadSafetyMode.NONE) {
        WalletCollectiblesAccountItemConverter(
            appCurrency = appCurrency,
            onAccountClick = clickIntents::onCollectiblesAccountClick,
        )
    }

    private val yieldSupplyPromoBannerConverter by lazy(LazyThreadSafetyMode.NONE) {
        YieldSupplyPromoBannerConverter(
            yieldModuleApyMap,
            shouldShowMainPromo,
        )
    }

    /**
     * Joint and collectibles accounts carry a row of their own while they hold no tokens, so an otherwise empty
     * wallet must not fall into the empty state and hide them
     */
    private val AccountStatus.isTokenlessAccountRow: Boolean
        get() = this is AccountStatus.Joint || this is AccountStatus.Collectibles && value.isShown

    @RemoveWithToggle(
        toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED",
        description = "Also backs legacy portfolio rows until AND_18663_REDESIGNED_ACCOUNT_ROWS_ENABLED is removed",
    )
    private fun currencyRowConverter(
        accountId: AccountId,
        shouldShowPromo: Boolean,
    ): WalletTokenCurrencyItemConverter {
        return WalletTokenCurrencyItemConverter(
            appCurrency = appCurrency,
            accountId = accountId,
            shouldShowPromo = shouldShowPromo,
            yieldModuleApyMap = yieldModuleApyMap,
            earnOverviews = earnOverviews,
            clickIntents = clickIntents,
            userWalletId = selectedWallet.walletId,
            jointPendingOperations = jointPendingOperations.filter { it.account.accountId == accountId },
        )
    }

    private fun tokenRowConverter(accountId: AccountId): WalletTokenRowConverter {
        return WalletTokenRowConverter(
            appCurrency = appCurrency,
            accountId = accountId,
            clickIntents = clickIntents,
            yieldModuleApyMap = yieldModuleApyMap,
            earnOverviews = earnOverviews,
            userWalletId = selectedWallet.walletId,
            jointPendingOperations = jointPendingOperations.filter { it.account.accountId == accountId },
        )
    }

    override fun convert(value: AccountStatusList): WalletTokensListUM {
        val promoCryptoCurrency = yieldSupplyPromoBannerConverter.convert2(value = value)
        val hasTokenlessAccountRow = value.accountStatuses.any { it.isTokenlessAccountRow }

        return if (value.flattenPersonalCurrencies().isEmpty() && !hasTokenlessAccountRow) {
            WalletTokensListUM.Empty(
                onEmptyClick = { clickIntents.onManageTokensClick(value.mainAccount.accountId) },
            )
        } else {
            val tokenListUM = value.accountStatuses
                .asSequence()
                .flatMap { accountStatus ->
                    when (accountStatus) {
                        is AccountStatus.Personal -> personalItems(accountStatus, promoCryptoCurrency)
                        is AccountStatus.Joint -> jointItems(accountStatus, promoCryptoCurrency)
                        is AccountStatus.Collectibles -> collectiblesItems(accountStatus)
                        is AccountStatus.Payment,
                        is AccountStatus.Money,
                        is AccountStatus.Prediction,
                        -> emptySequence()
                    }
                }.toPersistentList()

            WalletTokensListUM.Content(
                tokenList = tokenListUM,
                organizeButtonUM = getOrganizeButtonUM(value),
            )
        }
    }

    private fun personalItems(
        accountStatus: AccountStatus.Personal,
        promoCryptoCurrency: CryptoCurrencyStatus?,
    ): Sequence<TokensListItemUM2> {
        if (!isAccountsModeEnabled) {
            return if (isRedesignedTokenRowsEnabled) {
                getTokenListItemsV2(accountStatus, promoCryptoCurrency)
            } else {
                getTokenListItems(accountStatus, promoCryptoCurrency)
            }
        }
        return sequenceOf(portfolioItem(accountStatus, promoCryptoCurrency))
    }

    private fun collectiblesItems(accountStatus: AccountStatus.Collectibles): Sequence<TokensListItemUM2> {
        return listOfNotNull(collectiblesAccountRowConverter.convert(accountStatus)).asSequence()
    }

    private fun jointItems(
        accountStatus: AccountStatus.Joint,
        promoCryptoCurrency: CryptoCurrencyStatus?,
    ): Sequence<TokensListItemUM2> {
        return if (accountStatus.value is JointAccountStatusValue.Active) {
            sequenceOf(portfolioItem(accountStatus, promoCryptoCurrency))
        } else {
            sequenceOf(jointAccountRowConverter.convert(accountStatus))
        }
    }

    private fun portfolioItem(
        accountStatus: AccountStatus.CryptoPortfolio,
        promoCryptoCurrency: CryptoCurrencyStatus?,
    ): TokensListItemUM2 {
        if (isRedesignedAccountRowsEnabled) {
            return accountRowV2Converter.convert(
                value = accountStatus,
                tokenList = getTokenListItemsV2(accountStatus, promoCryptoCurrency).toPersistentList(),
            )
        }
        return TokensListItemUM2.Portfolio(
            tokenRowUM = accountRowConverter.convert(accountStatus),
            isExpanded = expandedAccounts.contains(accountStatus.account.accountId),
            isCollapsable = true,
            onEmptyClick = { clickIntents.onManageTokensClick(accountStatus.account.accountId) },
            tokenList = getTokenListItems(accountStatus, promoCryptoCurrency).toPersistentList(),
        )
    }

    @RemoveWithToggle(
        toggleName = "AND_18671_REDESIGNED_TOKEN_ROWS_ENABLED",
        description = "Also backs legacy portfolio rows until AND_18663_REDESIGNED_ACCOUNT_ROWS_ENABLED is removed",
    )
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

    private fun getTokenListItemsV2(
        accountStatus: AccountStatus.CryptoPortfolio,
        promoCryptoCurrency: CryptoCurrencyStatus?,
    ): Sequence<TokensListItemUM2> {
        val converter = tokenRowConverter(accountStatus.accountId)
        fun tokenItem(currencyStatus: CryptoCurrencyStatus): TokensListItemUM2.TokenV2 {
            val shouldShowPromo = promoCryptoCurrency?.currency?.id == currencyStatus.currency.id
            return converter.convert(currencyStatus, shouldShowPromo)
        }

        return when (val tokenList = accountStatus.tokenList) {
            TokenList.Empty -> emptySequence()
            is TokenList.GroupedByNetwork -> tokenList.groups.asSequence().flatMap { (network, currencies) ->
                sequenceOf(toSection(network)) + currencies.asSequence().map(::tokenItem)
            }
            is TokenList.Ungrouped -> tokenList.currencies.asSequence().map(::tokenItem)
        }
    }

    private fun toSection(network: Network): TokensListItemUM2.SectionV2 {
        val groupRow = toGroupRow(network)
        return TokensListItemUM2.SectionV2(id = groupRow.id, title = groupRow.title)
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
                            TangemTheme.colors3.icon.primary
                        } else {
                            TangemTheme.colors3.icon.tertiary
                        }
                    },
                ),
                onClick = clickIntents::onOrganizeTokensClick,
            )
        } else {
            null
        }
    }
}