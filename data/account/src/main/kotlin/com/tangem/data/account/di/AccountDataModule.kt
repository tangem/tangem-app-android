package com.tangem.data.account.di

import com.tangem.data.account.converter.AccountConverterFactoryContainer
import com.tangem.data.account.repository.DefaultAccountsCRUDRepository
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.ArchivedAccountsStoreFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AccountDataModule {

    @Provides
    @Singleton
    fun provideAccountsCRUDRepository(
        tangemTechApi: TangemTechApi,
        accountsResponseStoreFactory: AccountsResponseStoreFactory,
        userWalletsStore: UserWalletsStore,
        accountConverterFactoryContainer: AccountConverterFactoryContainer,
        dispatchers: CoroutineDispatcherProvider,
    ): AccountsCRUDRepository {
        return DefaultAccountsCRUDRepository(
            tangemTechApi = tangemTechApi,
            accountsResponseStoreFactory = accountsResponseStoreFactory,
            archivedAccountsStoreFactory = ArchivedAccountsStoreFactory,
            userWalletsStore = userWalletsStore,
            convertersContainer = accountConverterFactoryContainer,
            dispatchers = dispatchers,
        )
    }
}