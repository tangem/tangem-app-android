package com.tangem.features.commonfeatures.impl.portfolioselector

import com.tangem.common.ui.account.AccountPortfolioItemUMConverter
import com.tangem.common.ui.userwallet.converter.UserWalletItemUMConverter
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.impl.R
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorButtonUM
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorItemUM
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorUM
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
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<PortfolioSelectorComponent.Params>()
    private val balanceFetcher get() = params.portfolioFetcher
    private val selectorController get() = params.controller

    internal val state: StateFlow<PortfolioSelectorUM>
        field = MutableStateFlow<PortfolioSelectorUM>(emptyState())

    private val innerSelectedAccounts = MutableStateFlow(selectorController.selectedAccountsSync)

    init {
        combine(
            flow = isAccountsModeEnabledUseCase(),
            flow2 = balanceFetcher.data,
            flow3 = walletImageFetcher.allWallets(ArtworkSize.SMALL),
            flow4 = selectorController.isEnabled,
            flow5 = innerSelectedAccounts,
            transform = { isAccountsMode, portfolioData, artworks, isEnabled, selectedAccount ->
                val isAccountsModeEffective = isAccountsMode && !params.settings.isWalletSelectionOnly
                val uiList = buildUiList(
                    isAccountsMode = isAccountsModeEffective,
                    portfolioData = portfolioData,
                    artworks = artworks,
                    isEnabled = isEnabled,
                    selectedAccount = selectedAccount,
                )
                val title = when (isAccountsModeEffective) {
                    true -> resourceReference(R.string.common_choose_account)
                    false -> resourceReference(R.string.common_choose_wallet)
                }
                val button = PortfolioSelectorButtonUM(
                    text = resourceReference(R.string.common_apply),
                    onClick = { onApplyClick() },
                )
                state.value = PortfolioSelectorUM(
                    title = title,
                    items = uiList.toImmutableList(),
                    button = button.takeIf { params.settings.isMultiChoice },
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
        selectedAccount: Set<AccountId>,
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
        selectedAccount: Set<AccountId>,
    ): List<PortfolioSelectorItemUM> = buildList {
        val appCurrency = portfolioData.appCurrency
        val isBalanceHidden = portfolioData.isBalanceHidden
        val lockedWallets = mutableListOf<PortfolioSelectorItemUM>()
        portfolioData.balances.forEach { (_, portfolio) ->
            val balance = portfolio.walletBalance
            val wallet = portfolio.userWallet
            val mainAccount = portfolio.accountsBalance.mainAccount
            val isSelected = mainAccount.accountId in selectedAccount
            val endIcon = if (isSelected) {
                UserWalletItemUM.EndIcon.Checkmark
            } else {
                UserWalletItemUM.EndIcon.None
            }
            val walletItemUM = UserWalletItemUMConverter(
                onClick = { onWalletSelected(wallet.walletId, portfolioData, false) },
                appCurrency = appCurrency,
                balance = balance,
                isBalanceHidden = isBalanceHidden,
                artwork = artworks[wallet.walletId],
                isAuthMode = false,
                endIcon = endIcon,
                mode = UserWalletItemUMConverter.InfoField.Tokens(
                    tokensCount = portfolio.accountsBalance.flattenCurrencies().size,
                ),
            ).convert(wallet)
            if (walletItemUM.isEnabled) {
                val isEnabledByFeature = isEnabled(wallet, mainAccount)
                val finalWalletItemUM =
                    if (isEnabledByFeature) walletItemUM else walletItemUM.copy(isEnabled = false)
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
        selectedAccount: Set<AccountId>,
    ): List<PortfolioSelectorItemUM> = buildList {
        val appCurrency = portfolioData.appCurrency
        val isBalanceHidden = portfolioData.isBalanceHidden
        val lockedWallets = mutableListOf<PortfolioSelectorItemUM>()
        portfolioData.balances.forEach { (_, portfolio) ->
            val balance = portfolio.walletBalance
            val wallet = portfolio.userWallet
            val walletItemUM = UserWalletItemUMConverter(
                onClick = {
                    onAccountSelected(portfolio.accountsBalance.mainAccount.account.accountId)
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
                        val isAllSelected = portfolio.accountsBalance.accountStatuses
                            .all { accountStatus -> accountStatus.account.accountId in selectedAccount }
                        val walletTitle = PortfolioSelectorItemUM.GroupTitle(
                            id = "GroupTitle ${wallet.walletId.stringValue}",
                            name = stringReference(wallet.name),
                            isSelected = isAllSelected,
                            onClick = { onWalletSelected(wallet.walletId, portfolioData, true) },
                            deviceIcon = walletIconUMConverter.convert(getWalletIconUseCase(wallet)),
                        )

                        add(walletTitle)
                    }
                }
            }

            portfolio.accountsBalance.accountStatuses.filterCryptoPortfolio().forEach { accountStatus ->
                val isEnabledByFeature = isEnabled(wallet, accountStatus)
                val account = accountStatus.account
                val accountBalance = accountStatus.tokenList.totalFiatBalance
                val isSelected = accountStatus.accountId in selectedAccount
                val endIcon = if (isSelected) {
                    UserWalletItemUM.EndIcon.Checkmark
                } else {
                    UserWalletItemUM.EndIcon.None
                }
                val accountItemUM = AccountPortfolioItemUMConverter(
                    onClick = { onAccountSelected(account.accountId) },
                    appCurrency = appCurrency,
                    accountBalance = accountBalance,
                    isEnabled = isEnabledByFeature,
                    endIcon = endIcon,
                    isBalanceHidden = isBalanceHidden,
                ).convert(account)
                add(PortfolioSelectorItemUM.Portfolio(accountItemUM, isSelected))
            }
        }
        if (lockedWallets.isNotEmpty()) {
            addAll(createLockedWallets(wallets = lockedWallets))
        }
    }

    private fun onAccountSelected(accountId: AccountId) {
        if (params.settings.isMultiChoice) {
            innerSelectedAccounts.update { selected ->
                if (accountId in selected) {
                    selected - accountId
                } else {
                    selected + accountId
                }
            }
        } else {
            selectorController.selectAccount(accountId)
        }
    }

    // only isMultiChoice branch
    private fun onWalletSelected(
        walletId: UserWalletId,
        portfolioData: PortfolioFetcher.Data,
        isAccountMode: Boolean,
    ) {
        if (params.settings.isMultiChoice) {
            val portfolio = portfolioData.balances[walletId] ?: return
            innerSelectedAccounts.update { selected ->
                val isAllSelected = if (isAccountMode) {
                    portfolio.accountsBalance.accountStatuses
                        .all { accountStatus -> accountStatus.account.accountId in selected }
                } else {
                    portfolio.accountsBalance.mainAccount.account.accountId in selected
                }
                val walletAccounts = if (isAccountMode) {
                    portfolio.accountsBalance.accountStatuses
                        .mapTo(mutableSetOf()) { it.account.accountId }
                } else {
                    mutableSetOf(portfolio.accountsBalance.mainAccount.account.accountId)
                }
                if (isAllSelected) {
                    selected - walletAccounts
                } else {
                    selected + walletAccounts
                }
            }
        } else {
            portfolioData.balances[walletId]?.accountsBalance?.mainAccount?.account?.accountId
                ?.let { accountId -> selectorController.selectAccount(accountId) }
        }
    }

    private fun onApplyClick() {
        selectorController.selectAccount(innerSelectedAccounts.value)
    }

    private fun createLockedWallets(wallets: List<PortfolioSelectorItemUM>): List<PortfolioSelectorItemUM> {
        val lockedWalletsTitle = PortfolioSelectorItemUM.GroupTitle(
            id = "lockedWalletsTitleId",
            name = resourceReference(R.string.common_locked_wallets),
            deviceIcon = DeviceIconUM.Stub(cardsCount = 1),
            isSelected = false,
            onClick = {},
        )

        return listOf(lockedWalletsTitle) + wallets
    }

    private fun AccountStatus.isSelected(selectedId: AccountId?) = this.account.accountId == selectedId

    private fun emptyState() = PortfolioSelectorUM(
        items = persistentListOf(),
        title = TextReference.EMPTY,
        button = null,
    )
}