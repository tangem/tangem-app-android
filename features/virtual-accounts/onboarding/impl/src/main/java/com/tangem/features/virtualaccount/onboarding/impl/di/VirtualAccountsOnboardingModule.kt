package com.tangem.features.virtualaccount.onboarding.impl.di

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.virtualaccount.onboarding.api.VirtualAccountOnboardingComponent
import com.tangem.features.virtualaccount.onboarding.api.VirtualAccountsFeatureToggles
import com.tangem.features.virtualaccount.onboarding.impl.DefaultVirtualAccountOnboardingComponent
import com.tangem.features.virtualaccount.onboarding.impl.DefaultVirtualAccountsFeatureToggles
import com.tangem.features.virtualaccount.onboarding.impl.model.VirtualAccountOnboardingModel
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
internal object VirtualAccountsOnboardingModule {

    @Provides
    @Singleton
    fun provideVirtualAccountsFeatureToggles(
        featureTogglesManager: FeatureTogglesManager,
    ): VirtualAccountsFeatureToggles {
        return DefaultVirtualAccountsFeatureToggles(featureTogglesManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface VirtualAccountsOnboardingBindsModule {

    @Binds
    @Singleton
    fun bindVirtualAccountOnboardingComponentFactory(
        impl: DefaultVirtualAccountOnboardingComponent.Factory,
    ): VirtualAccountOnboardingComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface VirtualAccountsOnboardingModelModule {

    @Binds
    @IntoMap
    @ClassKey(VirtualAccountOnboardingModel::class)
    fun bindVirtualAccountOnboardingModel(model: VirtualAccountOnboardingModel): Model
}