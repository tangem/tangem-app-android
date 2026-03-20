package com.tangem.data.tokensync.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenSyncStoreFactory @Inject constructor(
    @NetworkMoshi private val moshi: Moshi,
    @ApplicationContext private val context: Context,
    private val appScope: AppCoroutineScope,
) {

    private val stores = ConcurrentHashMap<String, TokenSyncStore>()

    fun provide(userWalletId: UserWalletId): TokenSyncStore {
        val userWalletStringId = userWalletId.formatted()
        return stores.computeIfAbsent(userWalletStringId) {
            DefaultTokenSyncStore(
                persistenceStore = createPersistenceStore(
                    fileName = "token_sync_$userWalletStringId",
                    types = listTypes<UserTokensResponse.Token>(),
                    defaultValue = emptyList(),
                ),
            )
        }
    }

    private fun <T> createPersistenceStore(
        fileName: String,
        types: java.lang.reflect.ParameterizedType,
        defaultValue: T,
    ): DataStore<T> = DataStoreFactory.create(
        serializer = MoshiDataStoreSerializer(
            moshi = moshi,
            types = types,
            defaultValue = defaultValue,
        ),
        produceFile = { context.dataStoreFile(fileName = fileName) },
        scope = appScope,
    )

    private fun UserWalletId.formatted(): String = stringValue.lowercase()
}