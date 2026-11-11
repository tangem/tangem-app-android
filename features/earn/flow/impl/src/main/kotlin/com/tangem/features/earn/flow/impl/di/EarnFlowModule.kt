package com.tangem.features.earn.flow.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.earn.flow.api.EarnFlowComponent
import com.tangem.features.earn.flow.impl.DefaultEarnFlowComponent
import com.tangem.features.earn.flow.impl.model.EarnFlowModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface EarnFlowComponentModule {

    @Binds
    @Singleton
    fun bindFlowComponentFactory(impl: DefaultEarnFlowComponent.Factory): EarnFlowComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface EarnFlowModelModule {

    @Binds
    @IntoMap
    @ClassKey(EarnFlowModel::class)
    fun bindFlowModel(impl: EarnFlowModel): Model
}