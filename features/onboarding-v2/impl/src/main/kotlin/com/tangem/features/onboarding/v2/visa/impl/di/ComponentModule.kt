package com.tangem.features.onboarding.v2.visa.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.visa.api.OnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.DefaultOnboardingVisaComponent
import com.tangem.features.onboarding.v2.visa.impl.child.accesscode.model.OnboardingVisaAccessCodeModel
import com.tangem.features.onboarding.v2.visa.impl.child.approve.model.OnboardingVisaApproveModel
import com.tangem.features.onboarding.v2.visa.impl.child.choosewallet.model.OnboardingVisaChooseWalletModel
import com.tangem.features.onboarding.v2.visa.impl.child.inprogress.model.OnboardingVisaInProgressModel
import com.tangem.features.onboarding.v2.visa.impl.child.otherwallet.model.OnboardingVisaOtherWalletModel
import com.tangem.features.onboarding.v2.visa.impl.child.pincode.model.OnboardingVisaPinCodeModel
import com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.OnboardingVisaWelcomeModel
import com.tangem.features.onboarding.v2.visa.impl.model.OnboardingVisaModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    @Singleton
    fun bindComponentFactory(factory: DefaultOnboardingVisaComponent.Factory): OnboardingVisaComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnboardingVisaAccessCodeModel::class)
    fun provideModel(model: OnboardingVisaAccessCodeModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingVisaChooseWalletModel::class)
    fun provideModel2(model: OnboardingVisaChooseWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingVisaPinCodeModel::class)
    fun provideModel3(model: OnboardingVisaPinCodeModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingVisaApproveModel::class)
    fun provideModel4(model: OnboardingVisaApproveModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingVisaOtherWalletModel::class)
    fun provideModel5(model: OnboardingVisaOtherWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingVisaInProgressModel::class)
    fun provideModel6(model: OnboardingVisaInProgressModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingVisaModel::class)
    fun provideModel7(model: OnboardingVisaModel): Model

    @Binds
    @IntoMap
    @ClassKey(OnboardingVisaWelcomeModel::class)
    fun provideModel8(model: OnboardingVisaWelcomeModel): Model
}