package com.tangem.features.yield.supply.impl.promobottomsheet.di

import com.tangem.features.yield.supply.api.promobottomsheet.GetYieldModePromoVariantUseCase
import com.tangem.features.yield.supply.impl.promobottomsheet.DefaultGetYieldModePromoVariantUseCase
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface YieldSupplyPromoBottomSheetModule {

    @Binds
    fun bindGetYieldModePromoVariantUseCase(
        impl: DefaultGetYieldModePromoVariantUseCase,
    ): GetYieldModePromoVariantUseCase
}