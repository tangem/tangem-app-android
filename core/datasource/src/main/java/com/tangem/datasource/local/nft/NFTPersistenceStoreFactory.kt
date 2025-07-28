package com.tangem.datasource.local.nft

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.nft.custom.NFTPriceId
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import java.lang.reflect.ParameterizedType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NFTPersistenceStoreFactory @Inject constructor(
    @NetworkMoshi private val moshi: Moshi,
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    fun provide(userWalletId: UserWalletId, network: Network): NFTPersistenceStore {
        // simplify network identifier so that is correct for a file name
        // e.g. eth_m4460000 or theopennetwork_m446070
        val networkStringId =
            network.id.formatted() + network.derivationPath.formatted()?.let { "_$it" }.orEmpty()
        // simplify user wallet id so that is correct for a file name
        // e.g. 9a1a178f951a7115555568c09ebad8a882f3d96de25429f0017fe570931e208a
        val userWalletStringId = userWalletId.formatted()
        return DefaultNFTPersistenceStore(
            collectionsPersistenceStore = createPersistenceStore(
                // result file name example: nft_9a1a178f951a7115555568c09ebad8a882f3d96de25429f0017fe570931e208a_eth_m4460000_collections
                // result file name example: nft_9a1a178f951a7115555568c09ebad8a882f3d96de25429f0017fe570931e208a_theopennetwork_m446070_collections
                fileName = "nft_${userWalletStringId}_${networkStringId}_collections",
                types = listTypes<NFTCollection>(),
                defaultValue = emptyList(),
            ),
            pricesPersistenceStore = createPersistenceStore(
                // result file name example: nft_9a1a178f951a7115555568c09ebad8a882f3d96de25429f0017fe570931e208a_eth_m4460000_prices
                // result file name example: nft_9a1a178f951a7115555568c09ebad8a882f3d96de25429f0017fe570931e208a_theopennetwork_m446070_prices
                fileName = "nft_${userWalletStringId}_${networkStringId}_prices",
                types = listTypes<NFTPriceId>(),
                defaultValue = emptyList(),
            ),
        )
    }

    private fun <T> createPersistenceStore(fileName: String, types: ParameterizedType, defaultValue: T): DataStore<T> =
        DataStoreFactory.create(
            serializer = MoshiDataStoreSerializer(
                moshi = moshi,
                types = types,
                defaultValue = defaultValue,
            ),
            produceFile = { context.dataStoreFile(fileName = fileName) },
            scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
        )

    private fun Network.ID.formatted(): String = rawId.value
        .filter(Char::isLetterOrDigit)
        .lowercase()

    private fun Network.DerivationPath.formatted(): String? = value
        ?.filter(Char::isLetterOrDigit)
        ?.lowercase()

    private fun UserWalletId.formatted(): String = stringValue
        .lowercase()
}