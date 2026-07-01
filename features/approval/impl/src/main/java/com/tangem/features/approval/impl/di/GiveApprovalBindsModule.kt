package com.tangem.features.approval.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.approval.api.GiveApprovalComponent
import com.tangem.features.approval.api.GiveApprovalEntryComponent
import com.tangem.features.approval.api.GiveApprovalFeatureToggles
import com.tangem.features.approval.api.SelectApprovalTypeComponent
import com.tangem.features.approval.impl.DefaultGiveApprovalComponent
import com.tangem.features.approval.impl.DefaultGiveApprovalEntryComponent
import com.tangem.features.approval.impl.DefaultGiveApprovalFeatureToggles
import com.tangem.features.approval.impl.DefaultSelectApprovalTypeComponent
import com.tangem.features.approval.impl.model.GiveApprovalModel
import com.tangem.features.approval.impl.model.SelectApprovalTypeModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal interface GiveApprovalFeatureModule {

    @Singleton
    @Binds
    fun bindGiveApprovalFeatureToggle(toggles: DefaultGiveApprovalFeatureToggles): GiveApprovalFeatureToggles

    @Binds
    @Singleton
    fun bindComponentFactory(factory: DefaultGiveApprovalComponent.Factory): GiveApprovalComponent.Factory

    @Binds
    @Singleton
    fun bindSelectApprovalTypeComponentFactory(
        factory: DefaultSelectApprovalTypeComponent.Factory,
    ): SelectApprovalTypeComponent.Factory

    @Binds
    @Singleton
    fun bindGiveApprovalEntryComponentFactory(
        factory: DefaultGiveApprovalEntryComponent.Factory,
    ): GiveApprovalEntryComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface GiveApprovalModelModule {

    @Binds
    @IntoMap
    @ClassKey(GiveApprovalModel::class)
    fun bindModel(model: GiveApprovalModel): Model

    @Binds
    @IntoMap
    @ClassKey(SelectApprovalTypeModel::class)
    fun bindSelectApprovalTypeModel(model: SelectApprovalTypeModel): Model
}