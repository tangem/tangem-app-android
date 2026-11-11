package com.tangem.store.features.cardsettings.impl.coderecovery.di

import com.tangem.core.decompose.model.Model
import com.tangem.store.features.cardsettings.impl.coderecovery.DefaultAccessCodeRecoveryComponent
import com.tangem.store.features.cardsettings.impl.coderecovery.api.AccessCodeRecoveryComponent
import com.tangem.store.features.cardsettings.impl.coderecovery.model.AccessCodeRecoveryModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface AccessCodeRecoveryFeatureModule {

    @Binds
    fun bindFactory(impl: DefaultAccessCodeRecoveryComponent.Factory): AccessCodeRecoveryComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(AccessCodeRecoveryModel::class)
    fun bindModel(model: AccessCodeRecoveryModel): Model
}