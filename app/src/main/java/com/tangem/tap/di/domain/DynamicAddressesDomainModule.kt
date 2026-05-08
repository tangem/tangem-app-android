package com.tangem.tap.di.domain

import com.tangem.domain.dynamicaddresses.CreateConsolidationTransactionUseCase
import com.tangem.domain.dynamicaddresses.DisableDynamicAddressesUseCase
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.EnableDynamicAddressesUseCase
import com.tangem.domain.dynamicaddresses.GetDynamicAddressesStatusUseCase
import com.tangem.domain.dynamicaddresses.GetDynamicReceiveAddressUseCase
import com.tangem.domain.dynamicaddresses.GetDerivedXpubUseCase
import com.tangem.domain.dynamicaddresses.IsDynamicAddressesAvailableUseCase
import com.tangem.domain.dynamicaddresses.IsXpubSupportedUseCase
import com.tangem.domain.dynamicaddresses.repository.ConsolidationRepository
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.derivations.DerivationsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DynamicAddressesDomainModule {

    @Provides
    @Singleton
    fun provideEnableDynamicAddressesUseCase(
        dynamicAddressesRepository: DynamicAddressesRepository,
    ): EnableDynamicAddressesUseCase {
        return EnableDynamicAddressesUseCase(dynamicAddressesRepository)
    }

    @Provides
    @Singleton
    fun provideDisableDynamicAddressesUseCase(
        dynamicAddressesRepository: DynamicAddressesRepository,
    ): DisableDynamicAddressesUseCase {
        return DisableDynamicAddressesUseCase(dynamicAddressesRepository)
    }

    @Provides
    @Singleton
    fun provideGetDynamicAddressesStatusUseCase(
        dynamicAddressesRepository: DynamicAddressesRepository,
    ): GetDynamicAddressesStatusUseCase {
        return GetDynamicAddressesStatusUseCase(dynamicAddressesRepository)
    }

    @Provides
    @Singleton
    fun provideGetDynamicReceiveAddressUseCase(
        dynamicAddressesRepository: DynamicAddressesRepository,
    ): GetDynamicReceiveAddressUseCase {
        return GetDynamicReceiveAddressUseCase(dynamicAddressesRepository)
    }

    @Provides
    @Singleton
    fun provideCreateConsolidationTransactionUseCase(
        consolidationRepository: ConsolidationRepository,
    ): CreateConsolidationTransactionUseCase {
        return CreateConsolidationTransactionUseCase(consolidationRepository)
    }

    @Provides
    @Singleton
    fun provideIsXpubSupportedUseCase(walletManagersFacade: WalletManagersFacade): IsXpubSupportedUseCase {
        return IsXpubSupportedUseCase(walletManagersFacade)
    }

    @Provides
    @Singleton
    fun provideIsDynamicAddressesAvailableUseCase(
        featureToggles: DynamicAddressesFeatureToggles,
    ): IsDynamicAddressesAvailableUseCase {
        return IsDynamicAddressesAvailableUseCase(featureToggles)
    }

    @Provides
    @Singleton
    fun provideGetDerivedXpubUseCase(
        walletManagersFacade: WalletManagersFacade,
        derivationsRepository: DerivationsRepository,
    ): GetDerivedXpubUseCase {
        return GetDerivedXpubUseCase(walletManagersFacade, derivationsRepository)
    }
}