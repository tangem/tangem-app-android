package com.tangem.data.managetokens.di

import com.tangem.data.managetokens.DefaultCustomTokensRepository
import com.tangem.data.managetokens.DefaultManageTokensRepository
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.token.ExpressAssetsStore
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
        dispatchers: CoroutineDispatcherProvider,
        walletManagersFacade: WalletManagersFacade,
        tangemExpressApi: TangemExpressApi,
        expressAssetsStore: ExpressAssetsStore,
    ): ManageTokensRepository {
        return DefaultManageTokensRepository(
            tangemTechApi,
            userWalletsStore,
            manageTokensUpdateFetcher,
            appPreferencesStore,
            dispatchers,
            walletManagersFacade,
            tangemExpressApi,
            expressAssetsStore,
        )
    }

    @Provides
    @Singleton
    fun provideCustomTokensRepository(
        tangemTechApi: TangemTechApi,
        userWalletsStore: UserWalletsStore,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): CustomTokensRepository {
        return DefaultCustomTokensRepository(
            tangemTechApi,
            userWalletsStore,
            appPreferencesStore,
            dispatchers,
        )
    }
}