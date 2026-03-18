package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.row.header.TangemHeaderRowUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
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
import com.tangem.feature.wallet.presentation.wallet.state.utils.isSingleWalletWithToken
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
                                tokenRowUM = accountRowConverter.convert(accountStatus),
                                isExpanded = isExpanded || !isCollapsable,
                                isCollapsable = isCollapsable,
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

            WalletTokensListUM.Content(
                tokenList = tokenListUM,
                organizeButtonUM = getOrganizeButtonUM(value),
            )
        }
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
        return if (accountList.flattenCurrencies().size > 1 && !selectedWallet.isSingleWalletWithToken()) {
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
}