package com.tangem.feature.swap.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.swap.model.SwapModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface SwapModelModule {

    @Binds
    @IntoMap
    @ClassKey(SwapModel::class)
    fun provideSwapModel(model: SwapModel): Model
}