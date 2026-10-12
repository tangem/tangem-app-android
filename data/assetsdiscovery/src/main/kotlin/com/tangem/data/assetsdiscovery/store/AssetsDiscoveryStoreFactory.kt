package com.tangem.data.assetsdiscovery.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.core.remote.moshi.NetworkMoshi
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.create
import com.tangem.datasource.utils.listTypes
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssetsDiscoveryStoreFactory @Inject constructor(
    @NetworkMoshi private val moshi: Moshi,
    @ApplicationContext private val context: Context,
    private val appScope: AppCoroutineScope,
    private val dataStoreFactory: AppDataStoreFactory,
) {

    private val stores = ConcurrentHashMap<String, AssetsDiscoveryStore>()

    fun provide(userWalletId: UserWalletId): AssetsDiscoveryStore {
        val userWalletStringId = userWalletId.formatted()
        return stores.computeIfAbsent(userWalletStringId) {
            val fileName = "token_sync_$userWalletStringId"
            DefaultAssetsDiscoveryStore(
                persistenceStore = if (dataStoreFactory.isKotlinxDataStoreEnabled) {
                    dataStoreFactory.create(defaultValue = emptyList<UserTokensResponse.Token>(), fileName = fileName)
                } else {
                    createPersistenceStore(
                        fileName = fileName,
                        types = listTypes<UserTokensResponse.Token>(),
                        defaultValue = emptyList(),
                    )
                },
            )
        }
    }

    private fun <T> createPersistenceStore(
        fileName: String,
        types: java.lang.reflect.ParameterizedType,
        defaultValue: T,
    ): DataStore<T> = dataStoreFactory.create(
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