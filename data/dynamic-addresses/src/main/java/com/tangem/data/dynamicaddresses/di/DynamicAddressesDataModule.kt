package com.tangem.data.dynamicaddresses.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.account.WalletAccountsSaver
import com.tangem.data.dynamicaddresses.DefaultConsolidationRepository
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.data.dynamicaddresses.DefaultDynamicAddressesFeatureToggles
import com.tangem.data.dynamicaddresses.DefaultDynamicAddressesRepository
import com.tangem.domain.dynamicaddresses.DynamicAddressesFeatureToggles
import com.tangem.domain.dynamicaddresses.repository.ConsolidationRepository
import com.tangem.domain.dynamicaddresses.repository.DynamicAddressesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DynamicAddressesDataModule {

    @Provides
    @Singleton
    fun provideDynamicAddressesRepository(
        walletAccountsFetcher: WalletAccountsFetcher,
        walletAccountsSaver: WalletAccountsSaver,
        accountsCRUDRepository: AccountsCRUDRepository,
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
    ): DynamicAddressesRepository {
        return DefaultDynamicAddressesRepository(
            walletAccountsFetcher = walletAccountsFetcher,
            walletAccountsSaver = walletAccountsSaver,
            accountsCRUDRepository = accountsCRUDRepository,
            walletManagersFacade = walletManagersFacade,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideDynamicAddressesFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
    ): DynamicAddressesFeatureToggles {
        return DefaultDynamicAddressesFeatureToggles(featureTogglesManager = featureTogglesManager)
    }

    @Provides
    @Singleton
    fun provideConsolidationRepository(
        walletManagersFacade: WalletManagersFacade,
        dispatchers: CoroutineDispatcherProvider,
    ): ConsolidationRepository {
        return DefaultConsolidationRepository(
            walletManagersFacade = walletManagersFacade,
            dispatchers = dispatchers,
        )
    }
}