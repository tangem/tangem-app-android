package com.tangem.data.managetokens.di

import com.tangem.data.managetokens.DefaultCustomTokensRepository
import com.tangem.data.managetokens.DefaultManageTokensRepository
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.config.testnet.TestnetTokensStorage
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.managetokens.repository.ManageTokensRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ManageTokensDataModule {

    @Provides
    @Singleton
    fun provideManageTokensRepository(
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
        appPreferencesStore: AppPreferencesStore,
        testnetTokensStorage: TestnetTokensStorage,
        dispatchers: CoroutineDispatcherProvider,
    ): ManageTokensRepository {
        return DefaultManageTokensRepository(
            tangemTechApi,
            userWalletsStore,
            manageTokensUpdateFetcher,
            appPreferencesStore,
            testnetTokensStorage,
            dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideCustomTokensRepository(
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        appPreferencesStore: AppPreferencesStore,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
    ): CustomTokensRepository {
        return DefaultCustomTokensRepository(
            tangemTechApi,
            userWalletsStore,
            appPreferencesStore,
            walletManagersFacade,
            dispatchers,
        )
    }
}
