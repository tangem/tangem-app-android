package com.tangem.datasource.local.token

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.wallets.models.UserWalletId

internal class DefaultUserTokensStore(
    dataStore: StringKeyDataStore<UserTokensResponse>,
) : UserTokensStore, StringKeyDataStoreDecorator<UserWalletId, UserTokensResponse>(dataStore) {

    override fun provideStringKey(key: UserWalletId): String {
        return key.stringValue
    }
}
