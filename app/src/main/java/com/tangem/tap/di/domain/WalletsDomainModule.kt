package com.tangem.tap.di.domain

import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object WalletsDomainModule {

    @Provides
    @ViewModelScoped
    fun providesGetWalletsUseCase(walletsStateHolder: WalletsStateHolder): GetWalletsUseCase {
        return GetWalletsUseCase(walletsStateHolder = walletsStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesSaveWalletUseCase(walletsStateHolder: WalletsStateHolder): SaveWalletUseCase {
        return SaveWalletUseCase(walletsStateHolder = walletsStateHolder)
    }
}
