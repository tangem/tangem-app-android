package com.tangem.datasource.local.nft

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.nft.models.NFTAsset
import com.tangem.blockchain.nft.models.NFTCollection
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.listTypes
import com.tangem.datasource.utils.mapWithCustomKeyTypes
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class NFTStoreFactory(
    @NetworkMoshi private val moshi: Moshi,
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    fun provide(userWalletId: UserWalletId, blockchain: Blockchain): NFTStore {
        val blockchainKey = blockchain
            .id
            .replace("/", "_")
            .replace("-", "_")
            .lowercase()
        return BlockchainNFTStore(
            blockchain = blockchain,
            collectionsPersistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = listTypes<NFTCollection>(),
                    defaultValue = emptyList(),
                ),
                produceFile = {
                    context.dataStoreFile(fileName = "nft_${userWalletId}_${blockchainKey}_collections")
                },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
            collectionsRuntimeStore = RuntimeSharedStore(),
            pricesPersistenceStore = DataStoreFactory.create(
                serializer = MoshiDataStoreSerializer(
                    moshi = moshi,
                    types = mapWithCustomKeyTypes<NFTAsset.Identifier, NFTAsset.SalePrice>(),
                    defaultValue = emptyMap(),
                ),
                produceFile = {
                    context.dataStoreFile(fileName = "nft_${userWalletId}_${blockchainKey}_prices")
                },
                scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
            ),
            pricesRuntimeStore = RuntimeSharedStore(),
        )
    }
}
