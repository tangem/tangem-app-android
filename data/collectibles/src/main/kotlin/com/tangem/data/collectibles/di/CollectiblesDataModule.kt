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
import com.tangem.data.collectibles.flow.DefaultCollectiblesActiveTxsFetcher
import com.tangem.data.collectibles.flow.DefaultCollectiblesActiveTxsProducer
import com.tangem.data.collectibles.store.CollectiblesActiveTxsStore
import com.tangem.data.collectibles.store.CollectiblesMachineStore
import com.tangem.data.collectibles.store.DefaultCollectiblesDismissedTxRepository
import com.tangem.data.collectibles.store.DismissedTxsDTO
import com.tangem.data.collectibles.account.cleaner.CollectiblesUserWalletDataCleaner
import com.tangem.data.collectibles.account.derivation.DefaultCollectiblesOwnerDeriver
import com.tangem.data.collectibles.account.derivation.DefaultCollectiblesOwnerKeysRepository
import com.tangem.data.collectibles.account.flow.DefaultCollectiblesAccountStatusFetcher
import com.tangem.data.collectibles.account.flow.DefaultCollectiblesAccountStatusProducer
import com.tangem.data.collectibles.vault.DefaultCollectiblesVaultCurrencyFactory
import com.tangem.data.collectibles.vault.DefaultGachaVaultRepository
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusFetcher
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusProducer
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.domain.collectibles.account.repository.CollectiblesAccountRepository
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsFetcher
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsProducer
import com.tangem.domain.collectibles.flow.CollectiblesActiveTxsSupplier
import com.tangem.domain.collectibles.account.CollectiblesVaultCurrencyFactory
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerDeriver
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerKeysRepository
import com.tangem.domain.collectibles.vault.repository.GachaVaultRepository
import com.tangem.domain.collectibles.repository.CollectiblesDismissedTxRepository
import com.tangem.domain.collectibles.repository.CollectiblesRepository
import com.tangem.datasource.utils.AppDataStoreFactory
import com.tangem.datasource.utils.create
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
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectiblesDataModule {

    @Binds
    @Singleton
    fun bindCollectiblesOwnerDeriver(impl: DefaultCollectiblesOwnerDeriver): CollectiblesOwnerDeriver

    @Binds
    @Singleton
    fun bindCollectiblesOwnerKeysRepository(
        impl: DefaultCollectiblesOwnerKeysRepository,
    ): CollectiblesOwnerKeysRepository

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

    @Binds
    @Singleton
    fun bindCollectiblesActiveTxsProducerFactory(
        impl: DefaultCollectiblesActiveTxsProducer.Factory,
    ): CollectiblesActiveTxsProducer.Factory

    @Binds
    @Singleton
    fun bindCollectiblesActiveTxsFetcher(impl: DefaultCollectiblesActiveTxsFetcher): CollectiblesActiveTxsFetcher

    companion object {

        @Provides
        @Singleton
        fun provideCollectiblesActiveTxsStore(): CollectiblesActiveTxsStore {
            return CollectiblesActiveTxsStore(store = RuntimeSharedStore())
        }

        @Provides
        @Singleton
        fun provideCollectiblesActiveTxsSupplier(
            factory: CollectiblesActiveTxsProducer.Factory,
        ): CollectiblesActiveTxsSupplier {
            return object : CollectiblesActiveTxsSupplier(
                factory = factory,
                keyCreator = { params -> "collectibles_active_txs_${params.userWalletId.stringValue}" },
            ) {}
        }

        @Provides
        @Singleton
        fun provideCollectiblesMachineStore(): CollectiblesMachineStore {
            return CollectiblesMachineStore(store = RuntimeSharedStore())
        }

        @Provides
        @Singleton
        fun provideCollectiblesDismissedTxRepository(
            dataStoreFactory: AppDataStoreFactory,
        ): CollectiblesDismissedTxRepository {
            return DefaultCollectiblesDismissedTxRepository(
                store = dataStoreFactory.create(
                    defaultValue = DismissedTxsDTO(),
                    fileName = "collectibles_dismissed_txs",
                ),
            )
        }

        @Provides
        @CollectiblesRelinkTimeSource
        fun provideRelinkTimeSource(): TimeSource = TimeSource.Monotonic

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