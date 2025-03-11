package com.tangem.features.onramp.swap.availablepairs.di

import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onramp.swap.availablepairs.model.AvailableSwapPairsModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(DecomposeComponent::class)
internal interface AvailableSwapPairsModelModule {

    @Binds
    @IntoMap
    @ClassKey(AvailableSwapPairsModel::class)
    fun bindAvailableSwapPairsModel(model: AvailableSwapPairsModel): Model
}