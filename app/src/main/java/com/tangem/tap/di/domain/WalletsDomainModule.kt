package com.tangem.tap.di.domain

import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.usecase.ParseSharedAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletMemoUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.delegate.DefaultUserWalletsSyncDelegate
import com.tangem.domain.wallets.delegate.UserWalletsSyncDelegate
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.presentation.wallet.domain.IsWalletNFTEnabledSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNameMigrationUseCase
import com.tangem.operations.attestation.CardArtworksProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("TooManyFunctions")
@Module
@InstallIn(SingletonComponent::class)
internal object WalletsDomainModule {

    @Provides
    fun providesUserWalletsSyncDelegate(
        userWalletsListManager: UserWalletsListManager,
        dispatchers: CoroutineDispatcherProvider,
    ): UserWalletsSyncDelegate {
        return DefaultUserWalletsSyncDelegate(
            userWalletsListManager = userWalletsListManager,
            dispatchers = dispatchers,
        )
    }

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
        walletsRepository: WalletsRepository,
        userWalletsSyncDelegate: UserWalletsSyncDelegate,
    ): RenameWalletUseCase {
        return RenameWalletUseCase(
            walletsRepository = walletsRepository,
            userWalletsSyncDelegate = userWalletsSyncDelegate,
        )
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

    @Provides
    @Singleton
    fun providesSeedPhraseNotificationUseCase(walletsRepository: WalletsRepository): SeedPhraseNotificationUseCase {
        return SeedPhraseNotificationUseCase(walletsRepository = walletsRepository)
    }

    @Provides
    @Singleton
    fun provideGetCardImageUseCase(cardArtworksProvider: CardArtworksProvider): GetCardImageUseCase {
        return GetCardImageUseCase(cardArtworksProvider = cardArtworksProvider)
    }

    @Provides
    @Singleton
    fun providesIsWalletNFTEnabledSyncUseCase(walletsRepository: WalletsRepository): IsWalletNFTEnabledSyncUseCase {
        return IsWalletNFTEnabledSyncUseCase(walletsRepository = walletsRepository)
    }

    @Provides
    @Singleton
    fun providesUpdateRemoteWalletsInfoUseCase(
        walletsRepository: WalletsRepository,
        userWalletsSyncDelegate: UserWalletsSyncDelegate,
    ): UpdateRemoteWalletsInfoUseCase {
        return UpdateRemoteWalletsInfoUseCase(
            walletsRepository = walletsRepository,
            userWalletsSyncDelegate = userWalletsSyncDelegate,
        )
    }

    @Provides
    @Singleton
    fun providesGetSavedWalletChangesIdUseCase(
        userWalletsListManager: UserWalletsListManager,
    ): GetSavedWalletChangesUseCase {
        return GetSavedWalletChangesUseCase(
            userWalletsListManager = userWalletsListManager,
        )
    }

    @Provides
    @Singleton
    fun providesAssociateWalletsWithApplicationIdUseCase(
        walletsRepository: WalletsRepository,
    ): AssociateWalletsWithApplicationIdUseCase {
        return AssociateWalletsWithApplicationIdUseCase(
            walletsRepository = walletsRepository,
        )
    }

    @Provides
    @Singleton
    fun providesSetNotificationsEnabledUseCase(
        walletsRepository: WalletsRepository,
        currenciesRepository: CurrenciesRepository,
    ): SetNotificationsEnabledUseCase {
        return SetNotificationsEnabledUseCase(
            walletsRepository = walletsRepository,
            currenciesRepository = currenciesRepository,
        )
    }

    @Provides
    @Singleton
    fun providesGetWalletNotificationsEnabledUseCase(
        walletsRepository: WalletsRepository,
    ): GetWalletNotificationsEnabledUseCase {
        return GetWalletNotificationsEnabledUseCase(
            walletsRepository = walletsRepository,
        )
    }

    @Provides
    @Singleton
    fun providesGetIsNotificationsEnabledUseCase(
        walletsRepository: WalletsRepository,
    ): GetIsNotificationsEnabledUseCase {
        return GetIsNotificationsEnabledUseCase(
            walletsRepository = walletsRepository,
        )
    }
}