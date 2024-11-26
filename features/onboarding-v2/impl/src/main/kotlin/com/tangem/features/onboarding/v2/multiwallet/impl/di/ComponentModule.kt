package com.tangem.features.onboarding.v2.multiwallet.impl.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.multiwallet.api.OnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.DefaultOnboardingMultiWalletComponent
import com.tangem.features.onboarding.v2.multiwallet.impl.child.accesscode.model.MultiWalletAccessCodeModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.backup.model.MultiWalletBackupModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.chooseoption.model.Wallet1ChooseOptionModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.model.MultiWalletCreateWalletModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.finalize.model.MultiWalletFinalizeModel
import com.tangem.features.onboarding.v2.multiwallet.impl.child.seedphrase.model.MultiWalletSeedPhraseModel
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletModel
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
    fun bindComponent(factory: DefaultOnboardingMultiWalletComponent.Factory): OnboardingMultiWalletComponent.Factory
}

@Module
@InstallIn(DecomposeComponent::class)
internal interface ModelModule {

    @Binds
    @IntoMap
    @ClassKey(OnboardingMultiWalletModel::class)
    fun provideModel(model: OnboardingMultiWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(MultiWalletBackupModel::class)
    fun provideModel2(model: MultiWalletBackupModel): Model

    @Binds
    @IntoMap
    @ClassKey(MultiWalletCreateWalletModel::class)
    fun provideModel3(model: MultiWalletCreateWalletModel): Model

    @Binds
    @IntoMap
    @ClassKey(MultiWalletSeedPhraseModel::class)
    fun provideModel4(model: MultiWalletSeedPhraseModel): Model

    @Binds
    @IntoMap
    @ClassKey(MultiWalletAccessCodeModel::class)
    fun provideModel5(model: MultiWalletAccessCodeModel): Model

    @Binds
    @IntoMap
    @ClassKey(MultiWalletFinalizeModel::class)
    fun provideModel6(model: MultiWalletFinalizeModel): Model

    @Binds
    @IntoMap
    @ClassKey(Wallet1ChooseOptionModel::class)
    fun provideModel7(model: Wallet1ChooseOptionModel): Model
}