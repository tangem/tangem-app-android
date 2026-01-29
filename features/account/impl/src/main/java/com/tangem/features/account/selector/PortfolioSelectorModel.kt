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
import com.tangem.domain.models.account.AccountId
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
    walletImageFetcher: UserWalletImageFetcher,
    isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PortfolioSelectorComponent.Params>()
    private val balanceFetcher get() = params.portfolioFetcher
    private val selectorController get() = params.controller

    internal val state: StateFlow<PortfolioSelectorUM>
        field = MutableStateFlow<PortfolioSelectorUM>(emptyState())

    init {
        val selectedAccountState = selectorController.selectedAccount
            .stateIn(modelScope, started = SharingStarted.Eagerly, initialValue = null)

        combine(
            flow = isAccountsModeEnabledUseCase(),
            flow2 = balanceFetcher.data,
            flow3 = walletImageFetcher.allWallets(ArtworkSize.SMALL),
            flow4 = selectorController.isEnabled,
            flow5 = selectedAccountState,
            transform = { isAccountsMode, portfolioData, artworks, isEnabled, selectedAccount ->
                val uiList = buildUiList(
                    isAccountsMode = isAccountsMode,
                    portfolioData = portfolioData,
                    artworks = artworks,
                    isEnabled = isEnabled,
                    selectedAccount = selectedAccount,
                )
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
        selectedAccount: AccountId?,
    ): List<PortfolioSelectorItemUM> = when (isAccountsMode) {
        true -> buildAccountsList(
            portfolioData = portfolioData,
            artworks = artworks,
            isEnabled = isEnabled,
            selectedAccount = selectedAccount,
        )
        false -> buildWalletList(
            portfolioData = portfolioData,
            artworks = artworks,
            isEnabled = isEnabled,
            selectedAccount = selectedAccount,
        )
    }

    private fun buildWalletList(
        portfolioData: PortfolioFetcher.Data,
        artworks: Map<UserWalletId, UserWalletItemUM.ImageState>,
        isEnabled: (UserWallet, AccountStatus) -> Boolean,
        selectedAccount: AccountId?,
    ): List<PortfolioSelectorItemUM> = buildList {
        val appCurrency = portfolioData.appCurrency
        val isBalanceHidden = portfolioData.isBalanceHidden
        val lockedWallets = mutableListOf<PortfolioSelectorItemUM>()
        portfolioData.balances.forEach { (_, portfolio) ->
            val balance = portfolio.walletBalance
            val wallet = portfolio.userWallet
            val walletItemUM = UserWalletItemUMConverter(
                onClick = {
                    selectorController.selectAccount(portfolio.accountsBalance.mainAccount.account.accountId)
                },
                appCurrency = appCurrency,
                balance = balance,
                isBalanceHidden = isBalanceHidden,
                artwork = artworks[wallet.walletId],
                isAuthMode = false,
                mode = UserWalletItemUMConverter.InfoField.Tokens(
                    tokensCount = portfolio.accountsBalance.flattenCurrencies().size,
                ),
            ).convert(wallet)
            if (walletItemUM.isEnabled) {
                val mainAccount = portfolio.accountsBalance.mainAccount
                val isEnabledByFeature = isEnabled(wallet, mainAccount)
                val finalWalletItemUM =
                    if (isEnabledByFeature) walletItemUM else walletItemUM.copy(isEnabled = false)
                val isSelected = mainAccount.isSelected(selectedAccount)
                add(PortfolioSelectorItemUM.Portfolio(finalWalletItemUM, isSelected))
            } else {
                lockedWallets.add(PortfolioSelectorItemUM.Portfolio(walletItemUM, isSelected = false))
            }
        }

        if (lockedWallets.isNotEmpty()) {
            addAll(createLockedWallets(wallets = lockedWallets))
        }
    }

    private fun buildAccountsList(
        portfolioData: PortfolioFetcher.Data,
        artworks: Map<UserWalletId, UserWalletItemUM.ImageState>,
        isEnabled: (UserWallet, AccountStatus) -> Boolean,
        selectedAccount: AccountId?,
    ): List<PortfolioSelectorItemUM> = buildList {
        val appCurrency = portfolioData.appCurrency
        val isBalanceHidden = portfolioData.isBalanceHidden
        val lockedWallets = mutableListOf<PortfolioSelectorItemUM>()
        portfolioData.balances.forEach { (_, portfolio) ->
            val balance = portfolio.walletBalance
            val wallet = portfolio.userWallet
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
                lockedWallets.add(PortfolioSelectorItemUM.Portfolio(walletItemUM, false))
                return@forEach
            }

            when (balanceFetcher.mode.value) {
                // for Wallet mode expected single portfolioData.balances
                is PortfolioFetcher.Mode.Wallet -> Unit
                is PortfolioFetcher.Mode.All -> {
                    if (portfolioData.balances.size > 1) {
                        val walletTitle = PortfolioSelectorItemUM.GroupTitle(
                            id = "GroupTitle ${wallet.walletId.stringValue}",
                            name = stringReference(wallet.name),
                        )

                        add(walletTitle)
                    }
                }
            }

            portfolio.accountsBalance.accountStatuses.forEach { accountStatus ->
                val isEnabledByFeature = isEnabled(wallet, accountStatus)
                val account = accountStatus.account
                val accountBalance = when (accountStatus) {
                    is AccountStatus.Crypto.Portfolio -> accountStatus.tokenList.totalFiatBalance
                    is AccountStatus.Payment -> accountStatus.totalFiatBalance
                }
                val accountItemUM = AccountPortfolioItemUMConverter(
                    onClick = { selectorController.selectAccount(account.accountId) },
                    appCurrency = appCurrency,
                    accountBalance = accountBalance,
                    isEnabled = isEnabledByFeature,
                    isBalanceHidden = isBalanceHidden,
                ).convert(account)
                val isSelected = accountStatus.isSelected(selectedAccount)
                add(PortfolioSelectorItemUM.Portfolio(accountItemUM, isSelected))
            }
        }
        if (lockedWallets.isNotEmpty()) {
            addAll(createLockedWallets(wallets = lockedWallets))
        }
    }

    private fun createLockedWallets(wallets: List<PortfolioSelectorItemUM>): List<PortfolioSelectorItemUM> {
        val lockedWalletsTitle = PortfolioSelectorItemUM.GroupTitle(
            id = "lockedWalletsTitleId",
            name = resourceReference(R.string.common_locked_wallets),
        )

        return listOf(lockedWalletsTitle) + wallets
    }

    private fun AccountStatus.isSelected(selectedId: AccountId?) = this.account.accountId == selectedId

    private fun emptyState() = PortfolioSelectorUM(
        items = persistentListOf(),
        title = TextReference.EMPTY,
    )
}