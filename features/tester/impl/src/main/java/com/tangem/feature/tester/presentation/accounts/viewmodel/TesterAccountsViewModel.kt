package com.tangem.feature.tester.presentation.accounts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.tester.presentation.accounts.entity.AccountsUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class TesterAccountsViewModel @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val singleAccountListFetcher: SingleAccountListFetcher,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val eTagsStore: ETagsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : ViewModel() {

    val uiState: StateFlow<AccountsUM>
        field = MutableStateFlow(getInitialState())

    private val walletAccounts: StateFlow<Map<UserWalletId, AccountList>>
        field = MutableStateFlow(emptyMap())

    private var router: InnerTesterRouter? = null

    init {
        viewModelScope.launch {
            val userWallets = userWalletsListRepository.userWalletsSync()

            uiState.update { state ->
                state.copy(
                    walletSelector = state.walletSelector.copy(wallets = userWallets.toImmutableList()),
                )
            }

            userWallets.firstOrNull()?.let(::onWalletSelect)

            userWallets.map { userWallet ->
                singleAccountListSupplier(params = SingleAccountListProducer.Params(userWalletId = userWallet.walletId))
                    .distinctUntilChanged()
                    .onEach { accountList ->
                        walletAccounts.update { currentMap ->
                            currentMap.toMutableMap().apply {
                                this[userWallet.walletId] = accountList
                            }
                        }
                    }
                    .flowOn(dispatchers.default)
                    .launchIn(viewModelScope)
            }
        }
    }

    fun setupNavigation(router: InnerTesterRouter) {
        this.router = router
    }

    private fun getInitialState(): AccountsUM {
        return AccountsUM(
            onBackClick = { router?.back() },
            walletSelector = AccountsUM.WalletSelector(
                selected = null,
                wallets = persistentListOf(),
                onWalletSelect = ::onWalletSelect,
            ),
            accountListBottomSheetConfig = AccountsUM.AccountListBottomSheetConfig(
                isAccountsShown = false,
                accounts = persistentListOf(),
                onDismiss = {
                    uiState.update {
                        it.copy(
                            accountListBottomSheetConfig = it.accountListBottomSheetConfig.copy(
                                isAccountsShown = false,
                            ),
                        )
                    }
                },
            ),
            buttons = persistentListOf(
                AccountsUM.Button(id = AccountsUM.Button.ID.ShowAccountList, onClick = ::updateAccountsList),
                AccountsUM.Button(id = AccountsUM.Button.ID.FetchAccounts, onClick = ::fetchAccounts),
                AccountsUM.Button(id = AccountsUM.Button.ID.FillOutList, onClick = ::onFillOutClick),
                AccountsUM.Button(
                    id = AccountsUM.Button.ID.FillOutArchivedList,
                    onClick = ::fillOutArchivedAccountList,
                ),
                AccountsUM.Button(id = AccountsUM.Button.ID.ArchiveAll, onClick = ::onArchiveAllClick),
                AccountsUM.Button(id = AccountsUM.Button.ID.SortByDerivationIndex, onClick = ::sortAccountsByIndex),
                AccountsUM.Button(id = AccountsUM.Button.ID.ClearETag) { clearETag() },
            ),
        )
    }

    private fun onWalletSelect(userWallet: UserWallet) {
        uiState.update {
            it.copy(
                walletSelector = it.walletSelector.copy(selected = userWallet),
                accountListBottomSheetConfig = it.accountListBottomSheetConfig.copy(
                    accounts = getWalletAccounts(userWallet.walletId),
                ),
            )
        }
    }

    private fun updateAccountsList() {
        val userWalletId = getUserWallet()?.walletId ?: return
        val accounts = getWalletAccounts(userWalletId)

        uiState.update { state ->
            state.copy(
                accountListBottomSheetConfig = state.accountListBottomSheetConfig.copy(
                    isAccountsShown = true,
                    accounts = accounts,
                ),
            )
        }
    }

    private fun fetchAccounts() {
        val userWalletId = uiState.value.walletSelector.selected?.walletId ?: return

        withProgress(id = AccountsUM.Button.ID.FetchAccounts) {
            withContext(dispatchers.default) {
                singleAccountListFetcher(
                    params = SingleAccountListFetcher.Params(userWalletId = userWalletId),
                )
            }
        }
    }

    private fun clearETag() {
        viewModelScope.launch {
            val userWalletId = getUserWallet()?.walletId ?: return@launch
            eTagsStore.clear(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
        }
    }

    private fun onFillOutClick() {
        val userWalletId = getUserWallet()?.walletId ?: return

        withProgress(id = AccountsUM.Button.ID.FillOutList) {
            fillOutAccountList(userWalletId = userWalletId)
        }
    }

    private fun fillOutArchivedAccountList() {
        val userWalletId = getUserWallet()?.walletId ?: return
        val accountList = walletAccounts.value[userWalletId] ?: return
        if (accountList.totalArchivedAccounts == AccountList.MAX_ARCHIVED_ACCOUNTS_COUNT) return

        val id = AccountsUM.Button.ID.FillOutArchivedList

        viewModelScope.launch(dispatchers.default) {
            var currentTotalArchived = accountList.totalArchivedAccounts

            while (currentTotalArchived < AccountList.MAX_ARCHIVED_ACCOUNTS_COUNT) {
                updateButton(id) { button ->
                    button.copy(
                        title = "In progress... ($currentTotalArchived/${AccountList.MAX_ARCHIVED_ACCOUNTS_COUNT})",
                        isEnabled = false,
                    )
                }

                fillOutAccountList(userWalletId = userWalletId)
                archiveAll(userWalletId = userWalletId)

                accountsCRUDRepository.getAccountListSync(userWalletId).onSome {
                    currentTotalArchived = it.totalArchivedAccounts
                }
            }

            updateButton(id = id, button = AccountsUM.Button::reset)
        }
    }

    private suspend fun fillOutAccountList(userWalletId: UserWalletId) {
        withContext(dispatchers.default) {
            var accountList = walletAccounts.value[userWalletId] ?: return@withContext

            var nextIndex = accountList.totalAccounts
            @Suppress("LoopWithTooManyJumpStatements") // never mind for Tester Menu
            while (accountList.canAddMoreAccounts) {
                val derivationIndex = DerivationIndex(nextIndex).getOrNull() ?: break

                val newAccount = Account.Crypto.Portfolio.invoke(
                    accountId = AccountId.forCryptoPortfolio(userWalletId, derivationIndex),
                    name = "Account #$nextIndex",
                    icon = CryptoPortfolioIcon.ofDefaultCustomAccount(),
                    derivationIndex = nextIndex,
                    cryptoCurrencies = emptySet(),
                )
                    .getOrNull()
                    ?: break

                (accountList + newAccount)
                    .onRight { accountList = it }
                    .getOrNull()
                    ?: break

                nextIndex++
            }

            accountsCRUDRepository.saveAccounts(accountList)
        }
    }

    private fun onArchiveAllClick() {
        val userWalletId = getUserWallet()?.walletId ?: return

        withProgress(id = AccountsUM.Button.ID.ArchiveAll) {
            archiveAll(userWalletId = userWalletId)
        }
    }

    private suspend fun archiveAll(userWalletId: UserWalletId) {
        val accountList = walletAccounts.value[userWalletId] ?: return
        val possibleToArchive = AccountList.MAX_ARCHIVED_ACCOUNTS_COUNT - accountList.totalArchivedAccounts

        if (possibleToArchive == 0) return

        withContext(dispatchers.default) {
            val updatedAccountList = AccountList.invoke(
                userWalletId = accountList.userWalletId,
                accounts = if (possibleToArchive > AccountList.MAX_ACCOUNTS_COUNT - 1) {
                    listOf(accountList.mainAccount)
                } else {
                    accountList.accounts.subList(fromIndex = 0, toIndex = accountList.accounts.size - possibleToArchive)
                },
                totalAccounts = accountList.totalAccounts,
                totalArchivedAccounts = accountList.totalArchivedAccounts,
                sortType = accountList.sortType,
                groupType = accountList.groupType,
            )
                .getOrElse { return@withContext }

            accountsCRUDRepository.saveAccounts(accountList = updatedAccountList)
        }
    }

    private fun sortAccountsByIndex() {
        val userWalletId = getUserWallet()?.walletId ?: return
        val accountList = walletAccounts.value[userWalletId] ?: return

        withProgress(id = AccountsUM.Button.ID.SortByDerivationIndex) {
            withContext(dispatchers.default) {
                val sortedAccountList = AccountList.invoke(
                    userWalletId = accountList.userWalletId,
                    accounts = accountList.accounts
                        .filterIsInstance<Account.Crypto.Portfolio>()
                        .sortedBy { it.derivationIndex.value },
                    totalAccounts = accountList.totalAccounts,
                    totalArchivedAccounts = accountList.totalArchivedAccounts,
                    sortType = accountList.sortType,
                    groupType = accountList.groupType,
                )
                    .getOrElse { return@withContext }

                accountsCRUDRepository.saveAccounts(accountList = sortedAccountList)
            }
        }
    }

    private fun getUserWallet(): UserWallet? = uiState.value.walletSelector.selected

    private fun getWalletAccounts(userWalletId: UserWalletId): ImmutableList<Account.Crypto.Portfolio> {
        return walletAccounts.value[userWalletId]?.accounts
            ?.filterIsInstance<Account.Crypto.Portfolio>()
            ?.toImmutableList()
            ?: persistentListOf()
    }

    private fun withProgress(id: AccountsUM.Button.ID, block: suspend () -> Unit) {
        viewModelScope.launch {
            toggleButtonProgress(id = id, isInProgress = true)
            try {
                block()
            } finally {
                toggleButtonProgress(id = id, isInProgress = false)
            }
        }
    }

    private fun toggleButtonProgress(id: AccountsUM.Button.ID, isInProgress: Boolean) {
        updateButton(id) {
            it.copy(isInProgress = isInProgress)
        }
    }

    private fun updateButton(id: AccountsUM.Button.ID, button: (AccountsUM.Button) -> AccountsUM.Button) {
        uiState.update { state ->
            state.copy(
                buttons = state.buttons
                    .map { if (it.id == id) button(it) else it }
                    .toImmutableList(),
            )
        }
    }
}