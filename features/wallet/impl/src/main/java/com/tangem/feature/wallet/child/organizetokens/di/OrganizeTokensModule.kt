package com.tangem.feature.wallet.child.organizetokens.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.wallet.child.organizetokens.model.OrganizeTokensModel
import com.tangem.feature.wallet.child.organizetokens.model.OrganizeTokensModelLegacy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface OrganizeTokensModule {

    @Binds
    @IntoMap
    @ClassKey(OrganizeTokensModelLegacy::class)
    fun bindOrganizeTokensModelLegacy(model: OrganizeTokensModelLegacy): Model

    @Binds
    @IntoMap
    @ClassKey(OrganizeTokensModel::class)
    fun bindOrganizeTokensModel(model: OrganizeTokensModel): Model
}