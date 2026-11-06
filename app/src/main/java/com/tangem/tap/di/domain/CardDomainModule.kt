package com.tangem.tap.di.domain

import com.tangem.domain.card.BackupValidator
import com.tangem.domain.card.DeleteSavedAccessCodesUseCase
import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.card.ResetCardUseCase
import com.tangem.domain.card.ScanCardProcessor
import com.tangem.domain.card.SetCardWasScannedUseCase
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.collectibles.account.derivation.CollectiblesOwnerKeysRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.jointaccount.repository.JointOwnerKeysRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.usecase.*
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.tap.domain.card.DefaultResetCardUseCase
import com.tangem.tap.domain.scanCard.DefaultScanCardProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object CardDomainModule {

    @Provides
    fun provideScanCardProcessor(scanCardProcessor: DefaultScanCardProcessor): ScanCardProcessor {
        return scanCardProcessor
    }

    @Provides
    @Singleton
    fun provideSetCardWasScannedUseCase(cardRepository: CardRepository): SetCardWasScannedUseCase {
        return SetCardWasScannedUseCase(cardRepository = cardRepository)
    }

    @Provides
    @Singleton
    fun provideIsDemoCardUseCase(): IsDemoCardUseCase = IsDemoCardUseCase(config = DemoConfig)

    @Provides
    @Singleton
    fun provideBackupValidator(): BackupValidator = BackupValidator()

    @Provides
    @Singleton
    fun provideIsWalletBackupProblematicUseCase(backupValidator: BackupValidator): IsWalletBackupProblematicUseCase {
        return IsWalletBackupProblematicUseCase(backupValidator = backupValidator)
    }

    @Provides
    @Singleton
    fun provideDerivePublicKeysUseCase(derivationsRepository: DerivationsRepository): DerivePublicKeysUseCase {
        return DerivePublicKeysUseCase(derivationsRepository = derivationsRepository)
    }

    @Provides
    @Singleton
    fun provideDeriveCurrencyAndOwnerKeysUseCase(
        derivationsRepository: DerivationsRepository,
        jointOwnerKeysRepository: JointOwnerKeysRepository,
        collectiblesOwnerKeysRepository: CollectiblesOwnerKeysRepository,
    ): DeriveCurrencyAndOwnerKeysUseCase {
        return DeriveCurrencyAndOwnerKeysUseCase(
            derivationsRepository = derivationsRepository,
            jointOwnerKeysRepository = jointOwnerKeysRepository,
            collectiblesOwnerKeysRepository = collectiblesOwnerKeysRepository,
        )
    }

    @Provides
    fun provideIsNeedToBackupUseCase(userWalletsListRepository: UserWalletsListRepository): IsNeedToBackupUseCase {
        return IsNeedToBackupUseCase(userWalletsListRepository = userWalletsListRepository)
    }

    @Provides
    @Singleton
    fun provideGetExtendedPublicKeyForCurrencyUseCase(
        derivationsRepository: DerivationsRepository,
        walletManagersFacade: WalletManagersFacade,
    ): GetExtendedPublicKeyForCurrencyUseCase {
        return GetExtendedPublicKeyForCurrencyUseCase(derivationsRepository, walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideDeleteSavedAccessCodesUseCase(tangemSdkManager: TangemSdkManager): DeleteSavedAccessCodesUseCase {
        return DeleteSavedAccessCodesUseCase(tangemSdkManager)
    }

    @Provides
    @Singleton
    fun provideResetCardUseCase(tangemSdkManager: TangemSdkManager): ResetCardUseCase {
        return DefaultResetCardUseCase(tangemSdkManager)
    }

    @Provides
    @Singleton
    fun provideNetworkHasDerivationUseCase(): NetworkHasDerivationUseCase {
        return NetworkHasDerivationUseCase()
    }

    @Provides
    @Singleton
    fun provideIsRequiredDerivePublicKeysUseCase(
        derivationsRepository: DerivationsRepository,
    ): HasMissedDerivationsUseCase {
        return HasMissedDerivationsUseCase(derivationsRepository)
    }
}