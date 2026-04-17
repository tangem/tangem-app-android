package com.tangem.domain.account.status.utils

import com.tangem.domain.account.models.AccountExpandedState
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsExpandedRepository
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// todo swap separate for main and swap
@Singleton
class ExpandedAccountsHolder @Inject constructor(
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val accountsExpandedRepository: AccountsExpandedRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val actionChannel = MutableSharedFlow<Pair<AccountId, Boolean>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun expandedAccounts(userWallet: UserWallet): Flow<Set<AccountId>> = expandedAccounts(userWallet.walletId)

    fun expandedAccounts(walletId: UserWalletId): Flow<Set<AccountId>> = channelFlow {
        val storedState = accountsExpandedRepository.expandedAccounts
            .map { it[walletId].orEmpty() }
            .stateIn(this)

        val isAccountsMode = isAccountsModeEnabledUseCase.invoke()
            .stateIn(this)

        val initExpandedState = storedState.value
            .mapNotNull { it.takeIf { state -> state.isExpanded }?.accountId }
            .toSet()
        // main state holder
        val expandedAccounts = MutableStateFlow(initExpandedState)
        var debounceJob: Job? = null

        actionChannel
            .filter { (accountId, _) -> accountId.userWalletId == walletId }
            .filter { debounceJob?.isActive != true }
            .onEach { (accountId, isExpand) ->
                debounceJob = launch { delay(DEBOUNCE_MILLIS) }
                val newState = AccountExpandedState(accountId, isExpand)
                launch { accountsExpandedRepository.update(newState) }
                if (isExpand) {
                    expandedAccounts.update { it.plus(accountId) }
                } else {
                    expandedAccounts.update { it.minus(accountId) }
                }
            }
            .launchIn(this)

        walletAccounts(walletId).onEach { accountList ->
            if (!isAccountsModeEnabledUseCase.invokeSync()) {
                accountsExpandedRepository.clearStore()
                expandedAccounts.update { emptySet() }
                return@onEach
            }
            val idsSet = accountList.accounts.mapTo(mutableSetOf()) { it.accountId }
            accountsExpandedRepository.syncStore(walletId, idsSet)

            val isSingleAccount = accountList.accounts.size == 1
            val storedMainAccountState = storedState.value
                .find { it.accountId == accountList.mainAccount.accountId }

            if (isSingleAccount && storedMainAccountState == null) {
                // force expand for single and not stored account
                expandedAccounts.update { setOf(accountList.mainAccount.accountId) }
            }
        }.launchIn(this)

        combine(
            flow = expandedAccounts,
            flow2 = isAccountsMode,
            transform = { expanded, isAccountMode ->
                if (isAccountMode) {
                    channel.send(expanded)
                } else {
                    channel.send(emptySet())
                }
            },
        ).collect()
    }
        .flowOn(dispatchers.default)
        .distinctUntilChanged()

    fun expandAccount(accountId: AccountId) {
        actionChannel.tryEmit(accountId to true)
    }

    fun collapseAccount(accountId: AccountId) {
        actionChannel.tryEmit(accountId to false)
    }

    private fun walletAccounts(walletId: UserWalletId): Flow<AccountList> = singleAccountListSupplier(walletId)

    companion object {
        private const val DEBOUNCE_MILLIS = 200L
    }
}