package com.tangem.data.account.repository

import androidx.datastore.core.DataStore
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.data.account.converter.toAccountId
import com.tangem.domain.account.models.AccountExpandedState
import com.tangem.domain.account.repository.AccountsExpandedRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultAccountsExpandedRepository(
    private val store: DataStore<Map<String, Set<AccountsExpandedDTO>>>,
) : AccountsExpandedRepository {

    override val expandedAccounts: Flow<Map<UserWalletId, Set<AccountExpandedState>>> = store.data
        .map { stored ->
            stored.map { (rawWalletId, setOfDto) ->
                val walletId = UserWalletId(rawWalletId)
                val setOfState = setOfDto.mapTo(mutableSetOf()) { dto ->
                    AccountExpandedState(
                        accountId = dto.accountId.toAccountId(walletId),
                        isExpanded = dto.isExpanded,
                    )
                }
                walletId to setOfState
            }.toMap()
        }

    override suspend fun syncStore(walletId: UserWalletId, existAccounts: Set<AccountId>) {
        store.updateData { map ->
            val setOfDto = map[walletId.stringValue] ?: return@updateData map
            val existingAccountIds = existAccounts.map { it.value }.toSet()
            val syncedSet = setOfDto
                .filterTo(mutableSetOf()) { (accountId, _) -> existingAccountIds.contains(accountId) }

            map.plus(walletId.stringValue to syncedSet)
        }
    }

    override suspend fun clearStore() {
        store.updateData { emptyMap() }
    }

    override suspend fun update(accountState: AccountExpandedState) {
        store.updateData { map ->
            val walletId = accountState.accountId.userWalletId
            val setOfDto = map[walletId.stringValue].orEmpty()
            val newDto = AccountsExpandedDTO(
                accountId = accountState.accountId.value,
                isExpanded = accountState.isExpanded,
            )
            val updatedSet = setOfDto
                .filterTo(mutableSetOf()) { it.accountId != accountState.accountId.value }
                .plus(newDto)

            map.plus(walletId.stringValue to updatedSet)
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class AccountsExpandedDTO(
    @Json(name = "accountId")
    val accountId: String,
    @Json(name = "isExpanded")
    val isExpanded: Boolean,
)