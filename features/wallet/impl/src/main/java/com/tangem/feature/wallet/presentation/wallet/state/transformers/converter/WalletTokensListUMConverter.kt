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
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.JointAccountStatusValue
import com.tangem.domain.models.account.filterCryptoPortfolio
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
    private val accountIdsWithInvites: Set<String>,
    shouldShowMainPromo: Boolean,
) : Converter<AccountStatusList, WalletTokensListUM> {

    private val jointAccountRowConverter by lazy(LazyThreadSafetyMode.NONE) {
        WalletJointAccountItemConverter(
            appCurrency = appCurrency,
            userWalletId = selectedWallet.walletId,
            accountIdsWithInvites = accountIdsWithInvites,
            onAccountClick = clickIntents::onJointAccountClick,
        )
    }

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
        // A joint account carries a row of its own even while it holds nothing, so an otherwise empty wallet
        // must not fall into the empty state and hide it
        val hasJointAccount = value.accountStatuses.any { it is AccountStatus.Joint }
        return if (value.flattenPersonalCurrencies().isEmpty() && !hasJointAccount) {
            WalletTokensListUM.Empty(
                onEmptyClick = { clickIntents.onManageTokensClick(value.mainAccount.accountId) },
            )
        } else {
            // In the order the domain sorted the accounts: a joint account is an account, and appending its row
            // after everything else would move it away from where its list position says it belongs
            val tokenListUM = value.accountStatuses
                .filterCryptoPortfolio()
                .asSequence()
                .flatMap { accountStatus ->
                    when (accountStatus) {
                        is AccountStatus.Personal -> personalItems(accountStatus, promoCryptoCurrency)
                        is AccountStatus.Joint -> jointItems(accountStatus, promoCryptoCurrency)
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
        if (!isAccountsModeEnabled) return getTokenListItems(accountStatus, promoCryptoCurrency)
        return sequenceOf(portfolioItem(accountStatus, promoCryptoCurrency))
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
    ): TokensListItemUM2.Portfolio {
        return TokensListItemUM2.Portfolio(
            tokenRowUM = accountRowConverter.convert(accountStatus),
            isExpanded = expandedAccounts.contains(accountStatus.account.accountId),
            isCollapsable = true,
            onEmptyClick = { clickIntents.onManageTokensClick(accountStatus.account.accountId) },
            tokenList = getTokenListItems(accountStatus, promoCryptoCurrency).toPersistentList(),
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
}