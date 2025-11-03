package com.tangem.feature.tester.presentation.accounts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.tester.presentation.accounts.entity.AccountsUM
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class TesterAccountsViewModel @Inject constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
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
                accounts = persistentListOf(),
            ),
            onAccountsClick = ::updateAccountsList,
            onFetchAccountsClick = ::fetchAccounts,
            onClearETagClick = ::clearETag,
        )
    }

    private fun onWalletSelect(userWallet: UserWallet) {
        uiState.update {
            it.copy(
                walletSelector = it.walletSelector.copy(selected = userWallet),
                accountListBottomSheetConfig = it.accountListBottomSheetConfig.copy(
                    accounts = walletAccounts.value[userWallet.walletId]?.accounts
                        ?.filterIsInstance<Account.CryptoPortfolio>()
                        ?.toImmutableList()
                        ?: persistentListOf(),
                ),
            )
        }
    }

    private fun updateAccountsList(): Boolean {
        val userWalletId = uiState.value.walletSelector.selected?.walletId ?: return false

        val accounts = walletAccounts.value[userWalletId]?.accounts
            ?.filterIsInstance<Account.CryptoPortfolio>()
            ?.toImmutableList()
            ?: persistentListOf()

        uiState.update { state ->
            state.copy(
                accountListBottomSheetConfig = state.accountListBottomSheetConfig.copy(
                    accounts = accounts,
                ),
            )
        }

        return accounts.isEmpty()
    }

    private fun fetchAccounts() {
        viewModelScope.launch {
            val userWalletId = uiState.value.walletSelector.selected?.walletId ?: return@launch

            singleAccountListFetcher(
                params = SingleAccountListFetcher.Params(userWalletId = userWalletId),
            )
        }
    }

    private fun clearETag() {
        viewModelScope.launch {
            val userWallet = uiState.value.walletSelector.selected ?: return@launch
            eTagsStore.clear(userWalletId = userWallet.walletId, key = ETagsStore.Key.WalletAccounts)
        }
    }
}