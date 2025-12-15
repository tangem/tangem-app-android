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

            userWallets.mapIndexed { index, userWallet ->
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
                AccountsUM.Button(title = "Show the account list") { updateAccountsList() },
                AccountsUM.Button(title = "Fetch accounts", onClick = ::fetchAccounts),
                AccountsUM.Button(title = "Fill out the list (up to 20)", onClick = ::fillOutAccountList),
                AccountsUM.Button(title = "Archive all", onClick = ::archiveAllAccounts),
                AccountsUM.Button(title = "Sort by derivation index", onClick = ::sortAccountsByIndex),
                AccountsUM.Button(title = "Clear ETag") { clearETag() },
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

    private fun fetchAccounts(title: String) {
        viewModelScope.launch {
            val userWalletId = uiState.value.walletSelector.selected?.walletId ?: return@launch

            toggleButtonProgress(title = title, isInProgress = true)

            withContext(dispatchers.default) {
                singleAccountListFetcher(
                    params = SingleAccountListFetcher.Params(userWalletId = userWalletId),
                )
            }

            toggleButtonProgress(title = title, isInProgress = false)
        }
    }

    private fun clearETag() {
        viewModelScope.launch {
            val userWalletId = getUserWallet()?.walletId ?: return@launch
            eTagsStore.clear(userWalletId = userWalletId, key = ETagsStore.Key.WalletAccounts)
        }
    }

    private fun fillOutAccountList(title: String) {
        val userWalletId = getUserWallet()?.walletId ?: return

        viewModelScope.launch {
            toggleButtonProgress(title = title, isInProgress = true)

            withContext(dispatchers.default) {
                var accountList = walletAccounts.value[userWalletId] ?: return@withContext
                val occupiedIndexes = accountList.accounts
                    .filterIsInstance<Account.CryptoPortfolio>()
                    .map { it.derivationIndex.value }
                    .toSet()

                var nextIndex = 0
                @Suppress("LoopWithTooManyJumpStatements") // never mind for Tester Menu
                while (accountList.canAddMoreAccounts) {
                    while (occupiedIndexes.contains(nextIndex)) {
                        nextIndex++
                    }

                    val derivationIndex = DerivationIndex(nextIndex).getOrNull() ?: break

                    val newAccount = Account.CryptoPortfolio.invoke(
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

            toggleButtonProgress(title = title, isInProgress = false)
        }
    }

    private fun archiveAllAccounts(title: String) {
        val userWalletId = getUserWallet()?.walletId ?: return
        val accountList = walletAccounts.value[userWalletId] ?: return

        viewModelScope.launch {
            toggleButtonProgress(title = title, isInProgress = true)

            withContext(dispatchers.default) {
                val updatedAccountList = AccountList.invoke(
                    userWalletId = accountList.userWalletId,
                    accounts = listOf(accountList.mainAccount),
                    totalAccounts = accountList.totalAccounts,
                    sortType = accountList.sortType,
                    groupType = accountList.groupType,
                )
                    .getOrElse { return@withContext }

                accountsCRUDRepository.saveAccounts(accountList = updatedAccountList)
            }

            toggleButtonProgress(title = title, isInProgress = false)
        }
    }

    private fun sortAccountsByIndex(title: String) {
        val userWalletId = getUserWallet()?.walletId ?: return
        val accountList = walletAccounts.value[userWalletId] ?: return

        viewModelScope.launch {
            toggleButtonProgress(title = title, isInProgress = true)

            withContext(dispatchers.default) {
                val sortedAccountList = AccountList.invoke(
                    userWalletId = accountList.userWalletId,
                    accounts = accountList.accounts
                        .filterIsInstance<Account.CryptoPortfolio>()
                        .sortedBy { it.derivationIndex.value },
                    totalAccounts = accountList.totalAccounts,
                    sortType = accountList.sortType,
                    groupType = accountList.groupType,
                )
                    .getOrElse { return@withContext }

                accountsCRUDRepository.saveAccounts(accountList = sortedAccountList)
            }

            toggleButtonProgress(title = title, isInProgress = false)
        }
    }

    private fun getUserWallet(): UserWallet? = uiState.value.walletSelector.selected

    private fun getWalletAccounts(userWalletId: UserWalletId): ImmutableList<Account.CryptoPortfolio> {
        return walletAccounts.value[userWalletId]?.accounts
            ?.filterIsInstance<Account.CryptoPortfolio>()
            ?.toImmutableList()
            ?: persistentListOf()
    }

    private fun toggleButtonProgress(title: String, isInProgress: Boolean) {
        uiState.update { state ->
            state.copy(
                buttons = state.buttons.updateButton(title) {
                    it.copy(isInProgress = isInProgress)
                },
            )
        }
    }

    private fun ImmutableList<AccountsUM.Button>.updateButton(
        title: String,
        button: (AccountsUM.Button) -> AccountsUM.Button,
    ): ImmutableList<AccountsUM.Button> {
        return this
            .map { if (it.title == title) button(it) else it }
            .toImmutableList()
    }
}