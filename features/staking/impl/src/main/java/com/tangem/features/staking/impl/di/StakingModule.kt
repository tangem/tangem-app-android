package com.tangem.features.staking.impl.di

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.staking.api.StakingComponent
import com.tangem.features.staking.impl.DefaultStakingComponent
import com.tangem.features.staking.impl.navigation.DefaultStakingRouter
import com.tangem.features.staking.impl.navigation.InnerStakingRouter
import com.tangem.features.staking.impl.presentation.model.StakingModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface StakingModule {

    @Binds
    fun bindComponentFactory(factory: DefaultStakingComponent.Factory): StakingComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(StakingModel::class)
    fun bindModel(model: StakingModel): Model
}

@Module
@InstallIn(DecomposeComponent::class)
internal interface StakingComponentModule {

    @Binds
    @ComponentScoped
    fun bindRouter(impl: DefaultStakingRouter): InnerStakingRouter
}