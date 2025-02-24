package com.tangem.tap.features.details.ui.securitymode.di

import com.tangem.core.decompose.model.Model
import com.tangem.tap.features.details.ui.securitymode.DefaultSecurityModeComponent
import com.tangem.tap.features.details.ui.securitymode.api.SecurityModeComponent
import com.tangem.tap.features.details.ui.securitymode.model.SecurityModeModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface SecurityModeFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultSecurityModeComponent.Factory): SecurityModeComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(SecurityModeModel::class)
    fun bindModel(model: SecurityModeModel): Model
}