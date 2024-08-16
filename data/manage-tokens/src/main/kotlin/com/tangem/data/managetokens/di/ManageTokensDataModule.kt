package com.tangem.data.managetokens.di

import com.tangem.data.managetokens.DefaultManageTokensRepository
import com.tangem.data.managetokens.utils.ManageTokensUpdateFetcher
import com.tangem.data.managetokens.utils.ManagedCryptoCurrencyFactory
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.managetokens.repository.ManageTokensRepository
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
        managedCryptoCurrencyFactory: ManagedCryptoCurrencyFactory,
        manageTokensUpdateFetcher: ManageTokensUpdateFetcher,
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): ManageTokensRepository {
        return DefaultManageTokensRepository(
            tangemTechApi,
            userWalletsStore,
            managedCryptoCurrencyFactory,
            manageTokensUpdateFetcher,
            appPreferencesStore,
            dispatchers,
        )
    }
}
