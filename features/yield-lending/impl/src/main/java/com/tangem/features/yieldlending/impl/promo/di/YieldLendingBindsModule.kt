package com.tangem.features.yieldlending.impl.promo.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yieldlending.api.YieldLendingPromoComponent
import com.tangem.features.yieldlending.impl.promo.DefaultYieldLendingPromoComponent
import com.tangem.features.yieldlending.impl.promo.model.YieldLendingPromoModel
import com.tangem.features.yieldlending.impl.subcomponents.startearning.model.YieldLendingActionModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldLendingPromoBindsModule {
    @Binds
    @Singleton
    fun provideYieldLendingPromoComponentFactory(impl: DefaultYieldLendingPromoComponent.Factory): YieldLendingPromoComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface YieldLendingPromoModelModule {

    @Binds
    @IntoMap
    @ClassKey(YieldLendingPromoModel::class)
    fun provideYieldLendingPromoModel(impl: YieldLendingPromoModel): Model
}