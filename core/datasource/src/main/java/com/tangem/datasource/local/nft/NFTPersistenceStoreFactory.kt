package com.tangem.datasource.local.nft

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
import com.tangem.datasource.utils.mapWithCustomKeyTypes
import com.tangem.domain.tokens.model.Network
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NFTPersistenceStoreFactory @Inject constructor(
    @NetworkMoshi private val moshi: Moshi,
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    fun provide(network: Network): NFTPersistenceStore {
        val networkStringIdentifier = listOfNotNull(
            network.id.value,
            network.derivationPath.value,
        ).joinToString("_") {
            // remove all non-alphanumeric characters
            it
                .toCharArray()
                .filter(Char::isLetterOrDigit)
                .joinToString("")
                .lowercase()
        }
        return DefaultNFTPersistenceStore(
            collectionsPersistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = listTypes<NFTCollection>(),
                    defaultValue = emptyList(),
                ),
                produceFile = {
                    context.dataStoreFile(fileName = "nft_${networkStringIdentifier}_collections")
                },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
            pricesPersistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = mapWithCustomKeyTypes<NFTAsset.Identifier, NFTAsset.SalePrice>(),
                    defaultValue = emptyMap(),
                ),
                produceFile = {
                    context.dataStoreFile(fileName = "nft_${networkStringIdentifier}_prices")
                },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
        )
    }
}