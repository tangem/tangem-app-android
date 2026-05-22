package com.tangem.datasource.local.txhistory.store

import com.tangem.domain.models.account.AccountId
import kotlinx.serialization.Serializable

@Serializable
data class CommonSyncStateKey(
    val accountId: AccountId,
    val address: String,
)

@Serializable
data class CommonSyncState(
    val accountId: AccountId,
    val address: String,
    val isInitialCompleted: Boolean,
    val cursor: String?,
) {
    companion object {
        fun default(key: CommonSyncStateKey) = CommonSyncState(
            accountId = key.accountId,
            address = key.address,
            isInitialCompleted = false,
            cursor = null,
        )
    }
}