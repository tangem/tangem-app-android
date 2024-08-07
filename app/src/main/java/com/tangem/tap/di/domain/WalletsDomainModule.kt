package com.tangem.tap.di.domain

import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.usecase.ParseSharedAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletMemoUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNameMigrationUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object WalletsDomainModule {

    @Provides
    @Singleton
    fun providesGetWalletsUseCase(userWalletsListManager: UserWalletsListManager): GetWalletsUseCase {
        return GetWalletsUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesWalletNameMigrationUseCase(
        userWalletsListManager: UserWalletsListManager,
        walletNamesMigrationRepository: WalletNamesMigrationRepository,
    ): WalletNameMigrationUseCase {
        return WalletNameMigrationUseCase(
            userWalletsListManager = userWalletsListManager,
            walletNamesMigrationRepository = walletNamesMigrationRepository,
        )
    }

    @Provides
    @Singleton
    fun providesGetUserWalletUseCase(userWalletsListManager: UserWalletsListManager): GetUserWalletUseCase {
        return GetUserWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesGetSelectedWalletSyncUseCase(
        userWalletsListManager: UserWalletsListManager,
    ): GetSelectedWalletSyncUseCase {
        return GetSelectedWalletSyncUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesGetSelectedWalletUseCase(userWalletsListManager: UserWalletsListManager): GetSelectedWalletUseCase {
        return GetSelectedWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesSaveWalletUseCase(userWalletsListManager: UserWalletsListManager): SaveWalletUseCase {
        return SaveWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesGetExploreUrlUseCase(walletsManagersFacade: WalletManagersFacade): GetExploreUrlUseCase {
        return GetExploreUrlUseCase(walletsManagersFacade = walletsManagersFacade)
    }

    @Provides
    @Singleton
    fun providesUnlockWalletUseCase(userWalletsListManager: UserWalletsListManager): UnlockWalletsUseCase {
        return UnlockWalletsUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesSelectWalletUseCase(
        userWalletsListManager: UserWalletsListManager,
        reduxStateHolder: ReduxStateHolder,
    ): SelectWalletUseCase {
        return SelectWalletUseCase(userWalletsListManager = userWalletsListManager, reduxStateHolder = reduxStateHolder)
    }

    @Provides
    @Singleton
    fun providesUpdateWalletUseCase(userWalletsListManager: UserWalletsListManager): UpdateWalletUseCase {
        return UpdateWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesRenameWalletUseCase(
        userWalletsListManager: UserWalletsListManager,
        dispatchers: CoroutineDispatcherProvider,
    ): RenameWalletUseCase {
        return RenameWalletUseCase(userWalletsListManager = userWalletsListManager, dispatchers = dispatchers)
    }

    @Provides
    @Singleton
    fun providesGetWalletsSyncUseCase(userWalletsListManager: UserWalletsListManager): GetWalletNamesUseCase {
        return GetWalletNamesUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesDeleteWalletUseCase(userWalletsListManager: UserWalletsListManager): DeleteWalletUseCase {
        return DeleteWalletUseCase(userWalletsListManager = userWalletsListManager)
    }

    @Provides
    @Singleton
    fun providesShouldSaveUserWalletsSyncUseCase(
        walletsRepository: WalletsRepository,
    ): ShouldSaveUserWalletsSyncUseCase {
        return ShouldSaveUserWalletsSyncUseCase(walletsRepository = walletsRepository)
    }

    @Provides
    @Singleton
    fun providesShouldSaveUserWalletsUseCase(walletsRepository: WalletsRepository): ShouldSaveUserWalletsUseCase {
        return ShouldSaveUserWalletsUseCase(walletsRepository = walletsRepository)
    }

    @Provides
    @Singleton
    fun providesValidateWalletAddressUseCase(
        walletAddressServiceRepository: WalletAddressServiceRepository,
        walletManagersFacade: WalletManagersFacade,
    ): ValidateWalletAddressUseCase {
        return ValidateWalletAddressUseCase(
            walletAddressServiceRepository = walletAddressServiceRepository,
            walletManagersFacade = walletManagersFacade,
        )
    }

    @Provides
    @Singleton
    fun providesValidateWalletMemoUseCase(
        walletAddressServiceRepository: WalletAddressServiceRepository,
    ): ValidateWalletMemoUseCase {
        return ValidateWalletMemoUseCase(walletAddressServiceRepository = walletAddressServiceRepository)
    }

    @Provides
    @Singleton
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
