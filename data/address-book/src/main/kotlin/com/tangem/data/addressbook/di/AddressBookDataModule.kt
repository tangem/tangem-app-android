package com.tangem.data.addressbook.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.tangem.data.addressbook.DefaultAddressBookRepository
import com.tangem.data.addressbook.store.AddressBookBlobStore
import com.tangem.data.addressbook.store.DefaultAddressBookBlobStore
import com.tangem.data.addressbook.store.StoredAddressBookBlob
import com.tangem.datasource.utils.KotlinxDataStoreSerializer
import com.tangem.domain.addressbook.crypto.AddressBookCipher
import com.tangem.domain.addressbook.repository.AddressBookRepository
import com.tangem.domain.addressbook.time.IsoTimestampProvider
import com.tangem.domain.addressbook.usecase.GetContactsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AddressBookDataModule {

    @Provides
    @Singleton
    fun provideAddressBookBlobStore(
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
    ): AddressBookBlobStore {
        return DefaultAddressBookBlobStore(
            dataStore = DataStoreFactory.create(
                serializer = KotlinxDataStoreSerializer(
                    defaultValue = emptyMap(),
                    serializer = MapSerializer(
                        keySerializer = String.serializer(),
                        valueSerializer = StoredAddressBookBlob.serializer(),
                    ),
                ),
                produceFile = { context.dataStoreFile(fileName = "address_book_blobs") },
                scope = appScope,
            ),
        )
    }

    @Provides
    @Singleton
    fun provideAddressBookRepository(
        blobStore: AddressBookBlobStore,
        cipher: AddressBookCipher,
        userWalletsListRepository: UserWalletsListRepository,
        timestampProvider: IsoTimestampProvider,
        dispatchers: CoroutineDispatcherProvider,
    ): AddressBookRepository {
        return DefaultAddressBookRepository(
            blobStore = blobStore,
            cipher = cipher,
            userWalletsListRepository = userWalletsListRepository,
            timestampProvider = timestampProvider,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideGetContactsUseCase(repository: AddressBookRepository): GetContactsUseCase =
        GetContactsUseCase(repository)
}