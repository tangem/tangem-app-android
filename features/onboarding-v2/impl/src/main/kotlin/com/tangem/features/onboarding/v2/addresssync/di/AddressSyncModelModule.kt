package com.tangem.features.onboarding.v2.addresssync.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.onboarding.v2.addresssync.model.AddressSyncModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface AddressSyncModelModule {

    @Binds
    @IntoMap
    @ClassKey(AddressSyncModel::class)
    fun bindAddressSyncModel(model: AddressSyncModel): Model
}