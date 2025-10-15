package com.tangem.features.account.selector

import com.tangem.common.ui.account.AccountPortfolioItemUMConverter
import com.tangem.common.ui.userwallet.converter.UserWalletItemUMConverter
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.account.PortfolioFetcher
import com.tangem.features.account.PortfolioSelectorComponent
import com.tangem.features.account.impl.R
import com.tangem.features.account.selector.entity.PortfolioSelectorItemUM
import com.tangem.features.account.selector.entity.PortfolioSelectorUM
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.operations.attestation.ArtworkSize
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
internal class PortfolioSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val walletImageFetcher: UserWalletImageFetcher,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) : Model() {

    private val params = paramsContainer.require<PortfolioSelectorComponent.Params>()
    private val balanceFetcher get() = params.portfolioFetcher
    private val selectorController get() = params.controller

    internal val state: StateFlow<PortfolioSelectorUM>
        field = MutableStateFlow<PortfolioSelectorUM>(emptyState())

    init {
        combine(
            flow = isAccountsModeEnabledUseCase(),
            flow2 = balanceFetcher.data,
            flow3 = walletImageFetcher.allWallets(ArtworkSize.SMALL),
            flow4 = selectorController.isEnabled,
            transform = { isAccountsMode, portfolioData, artworks, isEnabled ->
                val uiList = buildUiList(isAccountsMode, portfolioData, artworks, isEnabled)
                val title = when (isAccountsMode) {
                    true -> resourceReference(R.string.common_choose_account)
                    false -> resourceReference(R.string.common_choose_wallet)
                }
                state.value = PortfolioSelectorUM(
                    title = title,
                    items = uiList.toImmutableList(),
                )
            },
        )
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun buildUiList(
        isAccountsMode: Boolean,
        portfolioData: PortfolioFetcher.Data,
        artworks: Map<UserWalletId, UserWalletItemUM.ImageState>,
        isEnabled: (UserWallet, AccountStatus) -> Boolean,
    ): List<PortfolioSelectorItemUM> = when (isAccountsMode) {
        true -> buildAccountsList(portfolioData, artworks, isEnabled)
        false -> buildWalletList(portfolioData, artworks, isEnabled)
    }

    private fun buildWalletList(
        portfolioData: PortfolioFetcher.Data,
        artworks: Map<UserWalletId, UserWalletItemUM.ImageState>,
        isEnabled: (UserWallet, AccountStatus) -> Boolean,
    ): List<PortfolioSelectorItemUM> = buildList {
        val appCurrency = portfolioData.appCurrency
        val isBalanceHidden = portfolioData.isBalanceHidden
        val lockedWallets = mutableListOf<PortfolioSelectorItemUM>()
        portfolioData.balances.forEach { wallet, portfolio ->
            val balance = portfolio.walletBalance
            val walletItemUM = UserWalletItemUMConverter(
                onClick = {
                    selectorController.selectAccount(portfolio.accountsBalance.mainAccount.account.accountId)
                },
                appCurrency = appCurrency,
                balance = balance,
                isBalanceHidden = isBalanceHidden,
                artwork = artworks[wallet.walletId],
                isAuthMode = false,
            ).convert(wallet)
            if (walletItemUM.isEnabled) {
                val isEnabledByFeature = isEnabled(wallet, portfolio.accountsBalance.mainAccount)
                val finalWalletItemUM =
                    if (isEnabledByFeature) walletItemUM else walletItemUM.copy(isEnabled = false)
                add(PortfolioSelectorItemUM.Portfolio(finalWalletItemUM))
            } else {
                lockedWallets.add(PortfolioSelectorItemUM.Portfolio(walletItemUM))
            }
        }
        if (lockedWallets.isNotEmpty()) {
            val lockedWalletsTitle = PortfolioSelectorItemUM.GroupTitle(
                id = "lockedWalletsTitleId",
                name = resourceReference(R.string.common_locked_wallets),
            )
            add(lockedWalletsTitle)
            addAll(lockedWallets)
        }
    }

    private fun buildAccountsList(
        portfolioData: PortfolioFetcher.Data,
        artworks: Map<UserWalletId, UserWalletItemUM.ImageState>,
        isEnabled: (UserWallet, AccountStatus) -> Boolean,
    ): List<PortfolioSelectorItemUM> = buildList {
        val appCurrency = portfolioData.appCurrency
        val isBalanceHidden = portfolioData.isBalanceHidden
        val lockedWallets = mutableListOf<PortfolioSelectorItemUM>()
        portfolioData.balances.forEach { wallet, portfolio ->
            val balance = portfolio.walletBalance
            val walletItemUM = UserWalletItemUMConverter(
                onClick = {
                    selectorController.selectAccount(portfolio.accountsBalance.mainAccount.account.accountId)
                },
                appCurrency = appCurrency,
                balance = balance,
                isBalanceHidden = isBalanceHidden,
                artwork = artworks[wallet.walletId],
                isAuthMode = false,
            ).convert(wallet)
            if (!walletItemUM.isEnabled) {
                lockedWallets.add(PortfolioSelectorItemUM.Portfolio(walletItemUM))
                return@forEach
            }

            val walletTitle = PortfolioSelectorItemUM.GroupTitle(
                id = "GroupTitle ${wallet.walletId.stringValue}",
                name = stringReference(wallet.name),
            )
            add(walletTitle)

            portfolio.accountsBalance.accountStatuses.forEach { accountStatus ->
                val isEnabledByFeature = isEnabled(wallet, accountStatus)
                val account = accountStatus.account
                val accountBalance = when (accountStatus) {
                    is AccountStatus.CryptoPortfolio -> accountStatus.tokenList.totalFiatBalance
                }
                val accountItemUM = AccountPortfolioItemUMConverter(
                    onClick = { selectorController.selectAccount(account.accountId) },
                    appCurrency = appCurrency,
                    accountBalance = accountBalance,
                    isEnabled = isEnabledByFeature,
                    isBalanceHidden = isBalanceHidden,
                ).convert(account)
                add(PortfolioSelectorItemUM.Portfolio(accountItemUM))
            }
        }
        if (lockedWallets.isNotEmpty()) {
            val lockedWalletsTitle = PortfolioSelectorItemUM.GroupTitle(
                id = "lockedWalletsTitleId",
                name = resourceReference(R.string.common_locked_wallets),
            )
            add(lockedWalletsTitle)
            addAll(lockedWallets)
        }
    }

    private fun emptyState() = PortfolioSelectorUM(
        items = persistentListOf(),
        title = TextReference.EMPTY,
    )
}