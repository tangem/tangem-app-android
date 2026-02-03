package com.tangem.data.account.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.account.converter.AccountConverterFactoryContainer
import com.tangem.data.account.featuretoggle.DefaultAccountsFeatureToggles
import com.tangem.data.account.fetcher.DefaultWalletAccountsFetcher
import com.tangem.data.account.repository.AccountsExpandedDTO
import com.tangem.data.account.repository.DefaultAccountsCRUDRepository
import com.tangem.data.account.repository.DefaultAccountsExpandedRepository
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.ArchivedAccountsStoreFactory
import com.tangem.data.account.tokens.DefaultMainAccountTokensMigration
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.accounts.AccountTokenMigrationStore
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.datasource.utils.MoshiDataStoreSerializer
import com.tangem.datasource.utils.mapWithStringKeyTypes
import com.tangem.datasource.utils.setTypes
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.repository.AccountsExpandedRepository
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountDataModule {

    @Provides
    @Singleton
    fun provideAccountFeatureToggle(featureTogglesManager: FeatureTogglesManager): AccountsFeatureToggles {
        return DefaultAccountsFeatureToggles(featureTogglesManager = featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideAccountsCRUDRepository(
        tangemTechApi: TangemTechApi,
        walletAccountsSaver: WalletAccountsSaver,
        accountsResponseStoreFactory: AccountsResponseStoreFactory,
        userTokensSaver: UserTokensSaver,
        accountConverterFactoryContainer: AccountConverterFactoryContainer,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): AccountsCRUDRepository {
        return DefaultAccountsCRUDRepository(
            tangemTechApi = tangemTechApi,
            walletAccountsSaver = walletAccountsSaver,
            accountsResponseStoreFactory = accountsResponseStoreFactory,
            archivedAccountsStoreFactory = ArchivedAccountsStoreFactory,
            userTokensSaver = userTokensSaver,
            archivedAccountsETagStore = RuntimeStateStore(emptyMap()),
            convertersContainer = accountConverterFactoryContainer,
            resources = context.resources,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideAccountsExpandedRepository(
        @NetworkMoshi moshi: Moshi,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
    ): AccountsExpandedRepository {
        val store = DataStoreFactory.create<Map<String, Set<AccountsExpandedDTO>>>(
            serializer = MoshiDataStoreSerializer(
                moshi = moshi,
                types = mapWithStringKeyTypes(valueTypes = setTypes<AccountsExpandedDTO>()),
                defaultValue = emptyMap(),
            ),
            produceFile = { context.dataStoreFile(fileName = "account_expanded_store") },
            scope = CoroutineScope(context = dispatchers.io + SupervisorJob()),
        )

        return DefaultAccountsExpandedRepository(
            store = store,
        )
    }

    @Provides
    @Singleton
    fun provideWalletAccountsFetcher(impl: DefaultWalletAccountsFetcher): WalletAccountsFetcher = impl

    @Provides
    @Singleton
    fun provideWalletAccountsSaver(impl: DefaultWalletAccountsFetcher): WalletAccountsSaver = impl

    @Provides
    @Singleton
    fun provideMainAccountTokensMigration(
        defaultMainAccountTokensMigration: DefaultMainAccountTokensMigration,
    ): MainAccountTokensMigration = defaultMainAccountTokensMigration

    @Provides
    @Singleton
    fun provideDefaultMainAccountTokensMigration(
        accountsResponseStoreFactory: AccountsResponseStoreFactory,
        userTokensSaver: UserTokensSaver,
        accountTokenMigrationStore: AccountTokenMigrationStore,
        eTagsStore: ETagsStore,
        dispatchers: CoroutineDispatcherProvider,
    ): DefaultMainAccountTokensMigration {
        return DefaultMainAccountTokensMigration(
            accountsResponseStoreFactory = accountsResponseStoreFactory,
            accountTokenMigrationStore = accountTokenMigrationStore,
            userTokensSaver = userTokensSaver,
            eTagsStore = eTagsStore,
            dispatchers = dispatchers,
        )
    }
}