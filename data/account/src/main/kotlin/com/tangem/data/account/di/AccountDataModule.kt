package com.tangem.data.account.di

import android.content.Context
import com.tangem.data.account.api.WalletAccountsApi
import com.tangem.data.account.cleaner.AccountsUserWalletDataCleaner
import com.tangem.data.account.converter.AccountConverterFactoryContainer
import com.tangem.data.account.fetcher.DefaultWalletAccountsFetcher
import com.tangem.data.account.repository.DefaultAccountsCRUDRepository
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.ArchivedAccountsStoreFactory
import com.tangem.data.account.tokens.DefaultMainAccountTokensMigration
import com.tangem.data.common.account.SupportedAccountTypes
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.data.common.currency.UserTokensSaver
import com.tangem.data.account.store.AccountTokenMigrationStore
import com.tangem.core.local.datastore.RuntimeStateStore
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.tokens.MainAccountTokensMigration
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountDataModule {

    @Provides
    @Singleton
    fun provideAccountsCRUDRepository(
        walletAccountsApi: WalletAccountsApi,
        walletAccountsSaver: WalletAccountsSaver,
        accountsResponseStoreFactory: AccountsResponseStoreFactory,
        accountConverterFactoryContainer: AccountConverterFactoryContainer,
        @ApplicationContext context: Context,
        dispatchers: CoroutineDispatcherProvider,
        supportedAccountTypes: SupportedAccountTypes,
    ): AccountsCRUDRepository {
        return DefaultAccountsCRUDRepository(
            walletAccountsApi = walletAccountsApi,
            walletAccountsSaver = walletAccountsSaver,
            accountsResponseStoreFactory = accountsResponseStoreFactory,
            archivedAccountsStoreFactory = ArchivedAccountsStoreFactory,
            archivedAccountsETagStore = RuntimeStateStore(emptyMap()),
            convertersContainer = accountConverterFactoryContainer,
            resources = context.resources,
            dispatchers = dispatchers,
            supportedAccountTypes = supportedAccountTypes,
        )
    }

    @Provides
    @IntoSet
    fun provideAccountsUserWalletDataCleaner(impl: AccountsUserWalletDataCleaner): UserWalletDataCleaner = impl

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
        appScope: AppCoroutineScope,
    ): DefaultMainAccountTokensMigration {
        return DefaultMainAccountTokensMigration(
            accountsResponseStoreFactory = accountsResponseStoreFactory,
            accountTokenMigrationStore = accountTokenMigrationStore,
            userTokensSaver = userTokensSaver,
            eTagsStore = eTagsStore,
            coroutineScope = appScope,
        )
    }
}