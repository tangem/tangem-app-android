package com.tangem.features.polymarket.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.ResolvePolymarketEntryUseCase
import com.tangem.features.polymarket.api.PolymarketComponent
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.polymarket.impl.DefaultPolymarketComponent
import com.tangem.features.polymarket.impl.featuretoggles.DefaultPolymarketFeatureToggles
import com.tangem.features.polymarket.impl.main.model.PolymarketMainModel
import com.tangem.features.polymarket.impl.model.PolymarketModel
import com.tangem.features.polymarket.impl.onboarding.model.PolymarketOnboardingModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface PolymarketBindsModule {
    @Binds
    @Singleton
    fun providePolymarketComponentFactory(impl: DefaultPolymarketComponent.Factory): PolymarketComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface PolymarketModelModule {

    @Binds
    @IntoMap
    @ClassKey(PolymarketModel::class)
    fun bindPolymarketModel(impl: PolymarketModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketMainModel::class)
    fun bindPolymarketMainModel(impl: PolymarketMainModel): Model

    @Binds
    @IntoMap
    @ClassKey(PolymarketOnboardingModel::class)
    fun bindPolymarketOnboardingModel(impl: PolymarketOnboardingModel): Model
}

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketFeatureTogglesModule {

    @Provides
    @Singleton
    fun providePolymarketFeatureToggles(featureTogglesManager: FeatureTogglesManager): PolymarketFeatureToggles {
        return DefaultPolymarketFeatureToggles(featureTogglesManager = featureTogglesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketDomainUseCasesModule {

    @Provides
    @Singleton
    fun provideResolvePolymarketEntryUseCase(
        checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
        derivePolymarketAddressesUseCase: DerivePolymarketAddressesUseCase,
        getPolymarketWalletStatusUseCase: GetPolymarketWalletStatusUseCase,
    ): ResolvePolymarketEntryUseCase = ResolvePolymarketEntryUseCase(
        checkPolymarketGeoblockUseCase = checkPolymarketGeoblockUseCase,
        derivePolymarketAddressesUseCase = derivePolymarketAddressesUseCase,
        getPolymarketWalletStatusUseCase = getPolymarketWalletStatusUseCase,
    )
}