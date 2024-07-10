package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

@Deprecated(
    message = "Use AppPreferencesStore",
    replaceWith = ReplaceWith(
        expression = "AppPreferencesStore",
        imports = arrayOf("com.tangem.datasource.local.preferences.AppPreferencesStore"),
    ),
    level = DeprecationLevel.WARNING,
)
interface UserTokensStore {

    @Deprecated(
        message = "Use getObject",
        replaceWith = ReplaceWith(
            expression = "appPreferencesStore.getObject(userWalletId)",
            imports = arrayOf("com.tangem.datasource.local.preferences.AppPreferencesStore"),
        ),
        level = DeprecationLevel.WARNING,
    )
    fun get(key: UserWalletId): Flow<UserTokensResponse>

    @Deprecated(
        message = "Use getObjectSyncOrNull",
        replaceWith = ReplaceWith(
            expression = "appPreferencesStore.getObjectSyncOrNull(userWalletId)",
            imports = arrayOf("com.tangem.datasource.local.preferences.AppPreferencesStore"),
        ),
        level = DeprecationLevel.WARNING,
    )
    suspend fun getSyncOrNull(key: UserWalletId): UserTokensResponse?

    @Deprecated(
        message = "Use storeObject",
        replaceWith = ReplaceWith(
            expression = "appPreferencesStore.storeObject(userWalletId, response)",
            imports = arrayOf("com.tangem.datasource.local.preferences.AppPreferencesStore"),
        ),
        level = DeprecationLevel.WARNING,
    )
    suspend fun store(key: UserWalletId, value: UserTokensResponse)
}