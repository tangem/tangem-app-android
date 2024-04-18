package com.tangem.tap.di.domain

import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
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
    fun providesGetWalletsUseCase(userWalletsListManager: UserWalletsListManager): GetWalletsUseCase {
        return GetWalletsUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @ViewModelScoped
    fun providesGetUserWalletUseCase(userWalletsListManager: UserWalletsListManager): GetUserWalletUseCase {
        return GetUserWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @ViewModelScoped
    fun providesGetSelectedWalletSyncUseCase(
        userWalletsListManager: UserWalletsListManager,
    ): GetSelectedWalletSyncUseCase {
        return GetSelectedWalletSyncUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @ViewModelScoped
    fun providesGetSelectedWalletUseCase(userWalletsListManager: UserWalletsListManager): GetSelectedWalletUseCase {
        return GetSelectedWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @ViewModelScoped
    fun providesSaveWalletUseCase(userWalletsListManager: UserWalletsListManager): SaveWalletUseCase {
        return SaveWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @ViewModelScoped
    fun providesGetExploreUrlUseCase(walletsManagersFacade: WalletManagersFacade): GetExploreUrlUseCase {
        return GetExploreUrlUseCase(walletsManagersFacade = walletsManagersFacade)
    }

    @Provides
    @ViewModelScoped
    fun providesUnlockWalletUseCase(userWalletsListManager: UserWalletsListManager): UnlockWalletsUseCase {
        return UnlockWalletsUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @ViewModelScoped
    fun providesSelectWalletUseCase(
        userWalletsListManager: UserWalletsListManager,
        reduxStateHolder: ReduxStateHolder,
    ): SelectWalletUseCase {
        return SelectWalletUseCase(userWalletsListManager = userWalletsListManager, reduxStateHolder = reduxStateHolder)
    }

    @Provides
    @ViewModelScoped
    fun providesUpdateWalletUseCase(userWalletsListManager: UserWalletsListManager): UpdateWalletUseCase {
        return UpdateWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @ViewModelScoped
    fun providesDeleteWalletUseCase(userWalletsListManager: UserWalletsListManager): DeleteWalletUseCase {
        return DeleteWalletUseCase(userWalletsListManager = userWalletsListManager)
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

    @Provides
    @ViewModelScoped
    fun providesValidateWalletMemoUseCase(
        walletAddressServiceRepository: WalletAddressServiceRepository,
    ): ValidateWalletMemoUseCase {
        return ValidateWalletMemoUseCase(walletAddressServiceRepository = walletAddressServiceRepository)
    }

    @Provides
    @ViewModelScoped
    fun providesParseSharedAddressUseCase(
        walletAddressServiceRepository: WalletAddressServiceRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ParseSharedAddressUseCase {
        return ParseSharedAddressUseCase(
            walletAddressServiceRepository = walletAddressServiceRepository,
            dispatchers = dispatchers,
        )
    }
}
