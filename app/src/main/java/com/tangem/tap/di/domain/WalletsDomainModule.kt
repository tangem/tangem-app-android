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
import com.tangem.domain.core.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.*
import com.tangem.feature.wallet.presentation.wallet.domain.IsWalletNFTEnabledSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNameMigrationUseCase
import com.tangem.features.hotwallet.HotWalletFeatureToggles
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
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
        dispatchers: CoroutineDispatcherProvider,
    ): UserWalletsSyncDelegate {
        return DefaultUserWalletsSyncDelegate(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun providesGetWalletsUseCase(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): GetWalletsUseCase {
        return GetWalletsUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewListRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
    }

    @Provides
    @Singleton
    fun providesWalletNameMigrationUseCase(
        userWalletsListManager: UserWalletsListManager,
        walletNamesMigrationRepository: WalletNamesMigrationRepository,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): WalletNameMigrationUseCase {
        return WalletNameMigrationUseCase(
            userWalletsListManager = userWalletsListManager,
            walletNamesMigrationRepository = walletNamesMigrationRepository,
            userWalletsListRepository = userWalletsListRepository,
            useNewListRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
    }

    @Provides
    @Singleton
    fun providesGetUserWalletUseCase(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): GetUserWalletUseCase {
        return GetUserWalletUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewListRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
    }

    @Provides
    @Singleton
    fun providesGetSelectedWalletSyncUseCase(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): GetSelectedWalletSyncUseCase {
        return GetSelectedWalletSyncUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
    }

    @Provides
    @Singleton
    fun providesGetSelectedWalletUseCase(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): GetSelectedWalletUseCase {
        return GetSelectedWalletUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
    }

    @Provides
    @Singleton
    fun providesSaveWalletUseCase(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
        walletsRepository: WalletsRepository,
    ): SaveWalletUseCase {
        return SaveWalletUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            walletsRepository = walletsRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
    }

    @Provides
    @Singleton
    fun providesOpenBuyTangemCardUseCase(): GenerateBuyTangemCardLinkUseCase {
        return GenerateBuyTangemCardLinkUseCase()
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
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
        reduxStateHolder: ReduxStateHolder,
    ): SelectWalletUseCase {
        return SelectWalletUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
            reduxStateHolder = reduxStateHolder,
        )
    }

    @Provides
    @Singleton
    fun providesUpdateWalletUseCase(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): UpdateWalletUseCase {
        return UpdateWalletUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
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
    fun providesGetWalletsSyncUseCase(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): GetWalletNamesUseCase {
        return GetWalletNamesUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
    }

    @Provides
    @Singleton
    fun providesDeleteWalletUseCase(
        userWalletsListManager: UserWalletsListManager,
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): DeleteWalletUseCase {
        return DeleteWalletUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
        )
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
        userWalletsListRepository: UserWalletsListRepository,
        hotWalletFeatureToggles: HotWalletFeatureToggles,
    ): GetSavedWalletsCountUseCase {
        return GetSavedWalletsCountUseCase(
            userWalletsListManager = userWalletsListManager,
            userWalletsListRepository = userWalletsListRepository,
            useNewRepository = hotWalletFeatureToggles.isHotWalletEnabled,
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