package com.tangem.tap.di.domain

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.usecase.ParseSharedAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletAddressUseCase
import com.tangem.domain.transaction.usecase.ValidateWalletMemoUseCase
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.delegate.DefaultUserWalletsSyncDelegate
import com.tangem.domain.wallets.delegate.UserWalletsSyncDelegate
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.domain.wallets.repository.WalletNamesMigrationRepository
import com.tangem.domain.wallets.repository.WalletsRepository
import com.tangem.domain.wallets.usecase.*
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyUpdateUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.IsWalletNFTEnabledSyncUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.WalletNameMigrationUseCase
import com.tangem.operations.attestation.CardArtworksProvider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Suppress("TooManyFunctions", "LargeClass")
@Module
@InstallIn(SingletonComponent::class)
internal object WalletsDomainModule {

    @Provides
    fun providesUserWalletsSyncDelegate(
        userWalletsListRepository: UserWalletsListRepository,
    ): UserWalletsSyncDelegate {
        return DefaultUserWalletsSyncDelegate(userWalletsListRepository = userWalletsListRepository)
    }

    @Provides
    @Singleton
    fun providesGetWalletsUseCase(userWalletsListRepository: UserWalletsListRepository): GetWalletsUseCase {
        return GetWalletsUseCase(userWalletsListRepository = userWalletsListRepository)
    }

    @Provides
    @Singleton
    fun providesWalletNameMigrationUseCase(
        walletNamesMigrationRepository: WalletNamesMigrationRepository,
        userWalletsListRepository: UserWalletsListRepository,
    ): WalletNameMigrationUseCase {
        return WalletNameMigrationUseCase(
            walletNamesMigrationRepository = walletNamesMigrationRepository,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    @Singleton
    fun providesGetUserWalletUseCase(userWalletsListRepository: UserWalletsListRepository): GetUserWalletUseCase {
        return GetUserWalletUseCase(userWalletsListRepository = userWalletsListRepository)
    }

    @Provides
    @Singleton
    fun providesGetSelectedWalletSyncUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): GetSelectedWalletSyncUseCase {
        return GetSelectedWalletSyncUseCase(userWalletsListRepository = userWalletsListRepository)
    }

    @Provides
    @Singleton
    fun providesGetSelectedWalletUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): GetSelectedWalletUseCase {
        return GetSelectedWalletUseCase(userWalletsListRepository = userWalletsListRepository)
    }

    @Provides
    @Singleton
    fun providesSaveWalletUseCase(
        userWalletsListRepository: UserWalletsListRepository,
        walletsRepository: WalletsRepository,
        analyticsEventHandler: AnalyticsEventHandler,
    ): SaveWalletUseCase {
        return SaveWalletUseCase(
            userWalletsListRepository = userWalletsListRepository,
            walletsRepository = walletsRepository,
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    @Provides
    @Singleton
    fun providesIsWalletAlreadySavedUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): IsWalletAlreadySavedUseCase {
        return IsWalletAlreadySavedUseCase(userWalletsListRepository = userWalletsListRepository)
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
    fun providesUnlockWalletUseCase(
        nonBiometricUnlockWalletUseCase: NonBiometricUnlockWalletUseCase,
        userWalletsListRepository: UserWalletsListRepository,
    ): UnlockWalletUseCase {
        return UnlockWalletUseCase(
            nonBiometricUnlockWalletUseCase = nonBiometricUnlockWalletUseCase,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    @Singleton
    fun providesNonBiometricUnlockWalletUseCase(
        userWalletsListRepository: UserWalletsListRepository,
        walletsRepository: WalletsRepository,
    ): NonBiometricUnlockWalletUseCase {
        return NonBiometricUnlockWalletUseCase(
            userWalletsListRepository = userWalletsListRepository,
            walletsRepository = walletsRepository,
        )
    }

    @Provides
    @Singleton
    fun providesSelectWalletUseCase(
        userWalletsListRepository: UserWalletsListRepository,
        reduxStateHolder: ReduxStateHolder,
    ): SelectWalletUseCase {
        return SelectWalletUseCase(
            userWalletsListRepository = userWalletsListRepository,
            reduxStateHolder = reduxStateHolder,
        )
    }

    @Provides
    @Singleton
    fun providesUpdateWalletUseCase(userWalletsListRepository: UserWalletsListRepository): UpdateWalletUseCase {
        return UpdateWalletUseCase(userWalletsListRepository = userWalletsListRepository)
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
    fun providesGetWalletsSyncUseCase(userWalletsListRepository: UserWalletsListRepository): GetWalletNamesUseCase {
        return GetWalletNamesUseCase(userWalletsListRepository = userWalletsListRepository)
    }

    @Provides
    @Singleton
    fun providesDeleteWalletUseCase(userWalletsListRepository: UserWalletsListRepository): DeleteWalletUseCase {
        return DeleteWalletUseCase(userWalletsListRepository = userWalletsListRepository)
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
        userWalletsListRepository: UserWalletsListRepository,
        generateWalletNameUseCase: GenerateWalletNameUseCase,
    ): UpdateRemoteWalletsInfoUseCase {
        return UpdateRemoteWalletsInfoUseCase(
            walletsRepository = walletsRepository,
            userWalletsSyncDelegate = userWalletsSyncDelegate,
            generateWalletNameUseCase = generateWalletNameUseCase,
            userWalletsListRepository = userWalletsListRepository,
        )
    }

    @Provides
    @Singleton
    fun providesGetSavedWalletChangesIdUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): GetSavedWalletsCountUseCase {
        return GetSavedWalletsCountUseCase(userWalletsListRepository = userWalletsListRepository)
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

    @Provides
    @Singleton
    fun providesUnlockHotWalletContextualUseCase(
        hotWalletAccessor: HotWalletAccessor,
    ): UnlockHotWalletContextualUseCase {
        return UnlockHotWalletContextualUseCase(
            hotWalletAccessor = hotWalletAccessor,
        )
    }

    @Provides
    @Singleton
    fun providesGetHotWalletContextualUnlockUseCase(
        hotWalletAccessor: HotWalletAccessor,
    ): GetHotWalletContextualUnlockUseCase {
        return GetHotWalletContextualUnlockUseCase(
            hotWalletAccessor = hotWalletAccessor,
        )
    }

    @Provides
    @Singleton
    fun providesClearHotWalletContextualUnlockUseCase(
        hotWalletAccessor: HotWalletAccessor,
    ): ClearHotWalletContextualUnlockUseCase {
        return ClearHotWalletContextualUnlockUseCase(
            hotWalletAccessor = hotWalletAccessor,
        )
    }

    @Provides
    @Singleton
    fun providesClearAllHotWalletContextualUnlockUseCase(
        hotWalletAccessor: HotWalletAccessor,
    ): ClearAllHotWalletContextualUnlockUseCase {
        return ClearAllHotWalletContextualUnlockUseCase(
            hotWalletAccessor = hotWalletAccessor,
        )
    }

    @Provides
    @Singleton
    fun providesExportSeedPhraseUseCase(hotWalletAccessor: HotWalletAccessor): ExportSeedPhraseUseCase {
        return ExportSeedPhraseUseCase(
            hotWalletAccessor = hotWalletAccessor,
        )
    }

    @Provides
    @Singleton
    fun providesColdWalletInteractionNeededUseCase(
        derivationsRepository: DerivationsRepository,
        getUserWalletUseCase: GetUserWalletUseCase,
    ): ColdWalletAndHasMissedDerivationsUseCase {
        return ColdWalletAndHasMissedDerivationsUseCase(
            derivationsRepository = derivationsRepository,
            userWalletUseCase = getUserWalletUseCase,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyApyFlowUseCase(yieldSupplyRepository: YieldSupplyRepository): YieldSupplyApyFlowUseCase {
        return YieldSupplyApyFlowUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideYieldSupplyApyUpdateUseCase(yieldSupplyRepository: YieldSupplyRepository): YieldSupplyApyUpdateUseCase {
        return YieldSupplyApyUpdateUseCase(
            yieldSupplyRepository = yieldSupplyRepository,
        )
    }

    @Provides
    @Singleton
    fun provideGetWalletsForAutomaticallyPushEnablingUseCase(
        userWalletsListRepository: UserWalletsListRepository,
        dispatcherProvider: CoroutineDispatcherProvider,
    ): GetWalletsForAutomaticallyPushEnablingUseCase {
        return GetWalletsForAutomaticallyPushEnablingUseCase(
            userWalletsListRepository = userWalletsListRepository,
            dispatchers = dispatcherProvider,
        )
    }

    @Provides
    @Singleton
    fun provideHasSecuredWalletsUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): HasSecuredWalletsUseCase {
        return HasSecuredWalletsUseCase(userWalletsListRepository = userWalletsListRepository)
    }

    @Provides
    @Singleton
    fun provideSyncWalletWithRemoteUseCase(walletsRepository: WalletsRepository): SyncWalletWithRemoteUseCase {
        return SyncWalletWithRemoteUseCase(walletsRepository = walletsRepository)
    }

    @Provides
    @Singleton
    fun provideApplyUserWalletListSortingUseCase(
        userWalletsListRepository: UserWalletsListRepository,
    ): ApplyUserWalletListSortingUseCase {
        return ApplyUserWalletListSortingUseCase(userWalletsListRepository = userWalletsListRepository)
    }
}