package com.tangem.data.collectibles.di

import android.content.Context
import androidx.datastore.dataStoreFile
import com.tangem.core.local.datastore.KotlinxDataStoreSerializer
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.DefaultCollectiblesRepository
import com.tangem.data.collectibles.account.DefaultCollectiblesAccountRepository
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.entity.CollectiblesAccountDTO
import com.tangem.data.collectibles.account.store.CollectiblesAccountStore
import com.tangem.data.collectibles.store.CollectiblesMachineStore
import com.tangem.data.collectibles.account.cleaner.CollectiblesUserWalletDataCleaner
import com.tangem.data.collectibles.account.derivation.DefaultCollectiblesOwnerDeriver
import com.tangem.data.collectibles.account.flow.DefaultCollectiblesAccountStatusFetcher
import com.tangem.data.collectibles.account.flow.DefaultCollectiblesAccountStatusProducer
import com.tangem.data.collectibles.vault.DefaultCollectiblesVaultCurrencyFactory
import com.tangem.data.collectibles.vault.DefaultGachaVaultRepository
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusProducer
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectiblesDataModule {

    @Binds
    @Singleton
    fun bindCollectiblesOwnerDeriver(impl: DefaultCollectiblesOwnerDeriver): CollectiblesOwnerDeriver

    @Binds
    @Singleton
    fun bindCollectiblesRepository(impl: DefaultCollectiblesRepository): CollectiblesRepository

    @Binds
    @Singleton
    fun bindCollectiblesAccountRepository(impl: DefaultCollectiblesAccountRepository): CollectiblesAccountRepository

    @Binds
    @IntoSet
    fun bindCollectiblesUserWalletDataCleaner(impl: CollectiblesUserWalletDataCleaner): UserWalletDataCleaner

    @Binds
    @Singleton
    fun bindGachaVaultRepository(impl: DefaultGachaVaultRepository): GachaVaultRepository

    @Binds
    @Singleton
    fun bindCollectiblesVaultCurrencyFactory(
        impl: DefaultCollectiblesVaultCurrencyFactory,
    ): CollectiblesVaultCurrencyFactory

    @Binds
    @Singleton
    fun bindCollectiblesAccountStatusProducerFactory(
        impl: DefaultCollectiblesAccountStatusProducer.Factory,
    ): CollectiblesAccountStatusProducer.Factory

    @Binds
    @Singleton
    fun bindCollectiblesAccountStatusFetcher(
        impl: DefaultCollectiblesAccountStatusFetcher,
    ): CollectiblesAccountStatusFetcher

    companion object {

        @Provides
        @Singleton
        fun provideCollectiblesMachineStore(): CollectiblesMachineStore {
            return CollectiblesMachineStore(store = RuntimeSharedStore())
        }

        @Provides
        @Singleton
        fun provideCollectiblesAccountStore(
            @ApplicationContext context: Context,
            scope: AppCoroutineScope,
            dataStoreFactory: AppDataStoreFactory,
            converter: CollectiblesAccountStatusValueDTOConverter,
        ): CollectiblesAccountStore {
            return CollectiblesAccountStore(
                runtimeStore = RuntimeSharedStore(),
                persistenceDataStore = dataStoreFactory.create(
                    serializer = KotlinxDataStoreSerializer(
                        defaultValue = emptyMap(),
                        serializer = MapSerializer(
                            keySerializer = String.serializer(),
                            valueSerializer = CollectiblesAccountDTO.serializer(),
                        ),
                        json = KotlinxDataStoreSerializer.jsonBuilder { classDiscriminator = "__type" },
                    ),
                    produceFile = { context.dataStoreFile(fileName = "collectibles_account_statuses") },
                    scope = scope,
                ),
                converter = converter,
                scope = scope,
            )
        }

        @Provides
        @Singleton
        fun provideCollectiblesAccountStatusSupplier(
            factory: CollectiblesAccountStatusProducer.Factory,
        ): CollectiblesAccountStatusSupplier {
            return object : CollectiblesAccountStatusSupplier(
                factory = factory,
                keyCreator = { params -> "collectibles_account_status_${params.userWalletId.stringValue}" },
            ) {}
        }
    }
}