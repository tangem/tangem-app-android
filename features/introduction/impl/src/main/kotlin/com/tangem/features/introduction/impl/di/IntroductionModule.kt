package com.tangem.features.introduction.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.introduction.AddWalletComponent
import com.tangem.features.introduction.IntroductionComponent
import com.tangem.features.introduction.impl.DefaultIntroductionComponent
import com.tangem.features.introduction.impl.addwallet.AddWalletModel
import com.tangem.features.introduction.impl.addwallet.DefaultAddWalletComponent
import com.tangem.features.introduction.impl.model.IntroductionModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface IntroductionComponentModule {

    @Binds
    @Singleton
    fun bindIntroductionComponentFactory(factory: DefaultIntroductionComponent.Factory): IntroductionComponent.Factory

    @Binds
    @Singleton
    fun bindAddWalletComponentFactory(factory: DefaultAddWalletComponent.Factory): AddWalletComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface IntroductionModelModule {

    @Binds
    @IntoMap
    @ClassKey(IntroductionModel::class)
    fun bindIntroductionModel(model: IntroductionModel): Model

    @Binds
    @IntoMap
    @ClassKey(AddWalletModel::class)
    fun bindAddWalletModel(model: AddWalletModel): Model
}