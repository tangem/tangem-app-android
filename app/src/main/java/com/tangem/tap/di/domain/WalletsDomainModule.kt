package com.tangem.tap.di.domain

import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.usecase.*
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
    fun providesGetSelectedWalletUseCase(walletsStateHolder: WalletsStateHolder): GetSelectedWalletUseCase {
        return GetSelectedWalletUseCase(walletsStateHolder = walletsStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesSaveWalletUseCase(walletsStateHolder: WalletsStateHolder): SaveWalletUseCase {
        return SaveWalletUseCase(walletsStateHolder = walletsStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesGetExploreUrlUseCase(walletsManagersFacade: WalletManagersFacade): GetExploreUrlUseCase {
        return GetExploreUrlUseCase(walletsManagersFacade = walletsManagersFacade)
    }

    @Provides
    @ViewModelScoped
    fun providesUnlockWalletUseCase(walletsStateHolder: WalletsStateHolder): UnlockWalletsUseCase {
        return UnlockWalletsUseCase(walletsStateHolder = walletsStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesSelectWalletUseCase(walletsStateHolder: WalletsStateHolder): SelectWalletUseCase {
        return SelectWalletUseCase(walletsStateHolder = walletsStateHolder)
    }
}
