package com.tangem.feature.tokendetails.di

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.di.DecomposeComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.tokendetails.DefaultTokenDetailsComponent
import com.tangem.feature.tokendetails.presentation.router.DefaultTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsModel
import com.tangem.features.tokendetails.TokenDetailsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenDetailsModule {

    @Binds
    fun bindComponentFactory(factory: DefaultTokenDetailsComponent.Factory): TokenDetailsComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(TokenDetailsModel::class)
    fun bindModel(model: TokenDetailsModel): Model
}

@Module
@InstallIn(DecomposeComponent::class)
internal interface StakingComponentModule {

    @Binds
    @ComponentScoped
    fun bindRouter(impl: DefaultTokenDetailsRouter): InnerTokenDetailsRouter
}