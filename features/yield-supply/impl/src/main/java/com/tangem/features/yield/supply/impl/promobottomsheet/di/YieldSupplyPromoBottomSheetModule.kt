package com.tangem.features.yield.supply.impl.promobottomsheet.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.api.promobottomsheet.GetYieldModePromoVariantUseCase
import com.tangem.features.yield.supply.api.promobottomsheet.ResolveYieldModePromoUseCase
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoBottomSheetComponent
import com.tangem.features.yield.supply.impl.promobottomsheet.DefaultGetYieldModePromoVariantUseCase
import com.tangem.features.yield.supply.impl.promobottomsheet.DefaultResolveYieldModePromoUseCase
import com.tangem.features.yield.supply.impl.promobottomsheet.DefaultYieldModePromoBottomSheetComponent
import com.tangem.features.yield.supply.impl.promobottomsheet.model.YieldModePromoBottomSheetModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldSupplyPromoBottomSheetModule {

    @Binds
    fun bindGetYieldModePromoVariantUseCase(
        impl: DefaultGetYieldModePromoVariantUseCase,
    ): GetYieldModePromoVariantUseCase

    @Binds
    fun bindResolveYieldModePromoUseCase(impl: DefaultResolveYieldModePromoUseCase): ResolveYieldModePromoUseCase

    @Binds
    @Singleton
    fun bindYieldModePromoBottomSheetComponentFactory(
        impl: DefaultYieldModePromoBottomSheetComponent.Factory,
    ): YieldModePromoBottomSheetComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyPromoBottomSheetModelModule {

    @Binds
    @IntoMap
    @ClassKey(YieldModePromoBottomSheetModel::class)
    fun bindYieldModePromoBottomSheetModel(impl: YieldModePromoBottomSheetModel): Model
}