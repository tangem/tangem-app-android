package com.tangem.domain.account.repository

import com.tangem.domain.account.models.AccountExpandedState
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface AccountsExpandedRepository {

    val expandedAccounts: Flow<Map<UserWalletId, Set<AccountExpandedState>>>

    suspend fun syncStore(walletId: UserWalletId, existAccounts: Set<AccountId>)
    suspend fun clearStore()
    suspend fun update(accountState: AccountExpandedState)

    interface Factory {
        fun create(storeFileName: String): AccountsExpandedRepository
    }

    companion object {
        const val MAIN_STORE_FILE_NAME = "account_expanded_store"
        const val CHOOSE_TOKEN_FILE_NAME = "choose_token_account_expanded_store"
    }
}