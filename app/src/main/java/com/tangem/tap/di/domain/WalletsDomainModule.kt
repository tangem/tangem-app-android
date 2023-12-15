package com.tangem.tap.di.domain

import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.repository.WalletAddressServiceRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
    fun providesGetUserWalletUseCase(walletsStateHolder: WalletsStateHolder): GetUserWalletUseCase {
        return GetUserWalletUseCase(walletsStateHolder = walletsStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesGetSelectedWalletSyncUseCase(walletsStateHolder: WalletsStateHolder): GetSelectedWalletSyncUseCase {
        return GetSelectedWalletSyncUseCase(walletsStateHolder = walletsStateHolder)
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
    fun providesSelectWalletUseCase(
        walletsStateHolder: WalletsStateHolder,
        reduxStateHolder: ReduxStateHolder,
    ): SelectWalletUseCase {
        return SelectWalletUseCase(walletsStateHolder, reduxStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesUpdateWalletUseCase(walletsStateHolder: WalletsStateHolder): UpdateWalletUseCase {
        return UpdateWalletUseCase(walletsStateHolder = walletsStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesDeleteWalletUseCase(walletsStateHolder: WalletsStateHolder): DeleteWalletUseCase {
        return DeleteWalletUseCase(walletsStateHolder = walletsStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesShouldSaveUserWalletsSyncUseCase(
        walletsRepository: WalletsRepository,
    ): ShouldSaveUserWalletsSyncUseCase {
        return ShouldSaveUserWalletsSyncUseCase(walletsRepository = walletsRepository)
    }

    @Provides
    @ViewModelScoped
    fun providesShouldSaveUserWalletsUseCase(walletsRepository: WalletsRepository): ShouldSaveUserWalletsUseCase {
        return ShouldSaveUserWalletsUseCase(walletsRepository = walletsRepository)
    }

    @Provides
    @ViewModelScoped
    fun providesValidateWalletAddressUseCase(
        walletAddressServiceRepository: WalletAddressServiceRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ValidateWalletAddressUseCase {
        return ValidateWalletAddressUseCase(
            walletAddressServiceRepository = walletAddressServiceRepository,
            dispatchers = dispatchers,
        )
    }
}