package com.tangem.tap.features.customtoken.impl.di

import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.features.customtoken.impl.data.DefaultCustomTokenRepository
import com.tangem.tap.features.customtoken.impl.domain.CustomTokenInteractor
import com.tangem.tap.features.customtoken.impl.domain.DefaultCustomTokenInteractor
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
[REDACTED_AUTHOR]
 */
@Module
@InstallIn(ViewModelComponent::class)
internal object CustomTokenInteractorModule {

    @Provides
    @ViewModelScoped
    fun provideCustomTokenInteractor(
        tangemTechApi: TangemTechApi,
        appCoroutineDispatcherProvider: AppCoroutineDispatcherProvider,
        reduxStateHolder: AppStateHolder,
        getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
        derivePublicKeysUseCase: DerivePublicKeysUseCase,
    ): CustomTokenInteractor {
        return DefaultCustomTokenInteractor(
            featureRepository = DefaultCustomTokenRepository(
                tangemTechApi = tangemTechApi,
                dispatchers = appCoroutineDispatcherProvider,
                reduxStateHolder = reduxStateHolder,
            ),
            getSelectedWalletSyncUseCase = getSelectedWalletSyncUseCase,
            derivePublicKeysUseCase = derivePublicKeysUseCase,
        )
    }
}