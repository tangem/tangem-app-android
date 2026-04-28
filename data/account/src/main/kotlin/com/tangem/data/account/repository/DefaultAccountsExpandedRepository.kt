package com.tangem.data.account.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.tangem.data.account.converter.toAccountId
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.datasource.utils.setTypes
import com.tangem.domain.account.models.AccountExpandedState
import com.tangem.domain.account.repository.AccountsExpandedRepository
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DefaultAccountsExpandedRepository constructor(
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
        store.updateData { map -> map.mapValues { emptySet() } }
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

    internal class Factory @Inject constructor(
        @NetworkMoshi private val moshi: Moshi,
        @ApplicationContext private val context: Context,
        private val appScope: AppCoroutineScope,
    ) : AccountsExpandedRepository.Factory {
        override fun create(storeFileName: String): DefaultAccountsExpandedRepository {
            val store = DataStoreFactory.create<Map<String, Set<AccountsExpandedDTO>>>(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = mapWithStringKeyTypes(valueTypes = setTypes<AccountsExpandedDTO>()),
                    defaultValue = emptyMap(),
                ),
                produceFile = { context.dataStoreFile(fileName = storeFileName) },
                scope = appScope,
            )

            return DefaultAccountsExpandedRepository(
                store = store,
            )
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