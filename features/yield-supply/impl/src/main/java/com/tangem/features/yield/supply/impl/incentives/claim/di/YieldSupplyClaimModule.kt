package com.tangem.features.yield.supply.impl.incentives.claim.di

import com.tangem.common.timemeasure.RealtimeMonotonicTimeSource
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.yield.supply.impl.incentives.claim.model.YieldSupplyClaimModel
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import kotlin.time.TimeSource

@Module
@InstallIn(ModelComponent::class)
internal interface YieldSupplyClaimModule {

    @Binds
    @IntoMap
    @ClassKey(YieldSupplyClaimModel::class)
    fun provideYieldSupplyClaimModel(impl: YieldSupplyClaimModel): Model

    companion object {

        /** Counts deep sleep too, unlike [TimeSource.Monotonic]: a proof goes stale while the device sleeps. */
        @Provides
        @YieldClaimTimeSource
        fun provideTimeSource(): TimeSource = RealtimeMonotonicTimeSource
    }
}