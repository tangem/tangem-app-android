package com.tangem.feature.wallet.presentation.account

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.models.AccountExpandedState
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.repository.AccountsExpandedRepository
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class ExpandedAccountsHolder @Inject constructor(
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val accountsExpandedRepository: AccountsExpandedRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    private val actionChannel = MutableSharedFlow<Pair<AccountId, Boolean>>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    fun expandedAccounts(userWallet: UserWallet): Flow<Set<AccountId>> = channelFlow {
        val walletId = userWallet.walletId

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

        actionChannel
            .filter { (accountId, _) -> accountId.userWalletId == walletId }
            .onEach { (accountId, isExpand) ->
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
            val idsSet = accountList.accounts.mapTo(mutableSetOf()) { it.accountId }
            accountsExpandedRepository.syncStore(walletId, idsSet)

            if (!isAccountsMode.value) return@onEach

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
                    channel.send(setOf())
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
}