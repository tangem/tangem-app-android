package com.tangem.data.account.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.account.converter.AccountConverterFactoryContainer
import com.tangem.data.account.featuretoggle.DefaultAccountsFeatureToggles
import com.tangem.data.account.fetcher.DefaultWalletAccountsFetcher
import com.tangem.data.account.repository.DefaultAccountsCRUDRepository
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.account.store.ArchivedAccountsStoreFactory
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.common.cache.etag.ETagsStore
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
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
    fun provideAccountFeatureToggle(featureTogglesManager: FeatureTogglesManager): AccountsFeatureToggles {
        return DefaultAccountsFeatureToggles(featureTogglesManager = featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideAccountsCRUDRepository(
        tangemTechApi: TangemTechApi,
        walletAccountsSaver: WalletAccountsSaver,
        accountsResponseStoreFactory: AccountsResponseStoreFactory,
        userWalletsStore: UserWalletsStore,
        eTagsStore: ETagsStore,
        accountConverterFactoryContainer: AccountConverterFactoryContainer,
        dispatchers: CoroutineDispatcherProvider,
    ): AccountsCRUDRepository {
        return DefaultAccountsCRUDRepository(
            tangemTechApi = tangemTechApi,
            walletAccountsSaver = walletAccountsSaver,
            accountsResponseStoreFactory = accountsResponseStoreFactory,
            archivedAccountsStoreFactory = ArchivedAccountsStoreFactory,
            userWalletsStore = userWalletsStore,
            eTagsStore = eTagsStore,
            convertersContainer = accountConverterFactoryContainer,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideWalletAccountsFetcher(impl: DefaultWalletAccountsFetcher): WalletAccountsFetcher = impl

    @Provides
    @Singleton
    fun provideWalletAccountsSaver(impl: DefaultWalletAccountsFetcher): WalletAccountsSaver = impl
}