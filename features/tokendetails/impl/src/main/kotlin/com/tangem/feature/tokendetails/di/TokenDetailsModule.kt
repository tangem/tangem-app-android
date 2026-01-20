package com.tangem.feature.tokendetails.di

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.feature.tokendetails.DefaultTokenDetailsComponent
import com.tangem.feature.tokendetails.presentation.DefaultExpressTransactionsComponent
import com.tangem.feature.tokendetails.presentation.router.DefaultTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.router.InnerTokenDetailsRouter
import com.tangem.feature.tokendetails.presentation.tokendetails.model.ExpressTransactionsModel
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsModel
import com.tangem.feature.tokendetails.presentation.tokendetails.state.utils.DefaultExpressTransactionsEventListener
import com.tangem.features.tokendetails.ExpressTransactionsComponent
import com.tangem.features.tokendetails.ExpressTransactionsEventListener
import com.tangem.features.tokendetails.TokenDetailsComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface TokenDetailsModule {

    @Binds
    fun bindTokenDetailsComponentFactory(factory: DefaultTokenDetailsComponent.Factory): TokenDetailsComponent.Factory

    @Binds
    fun bindExpressTransactionsComponent(
        factory: DefaultExpressTransactionsComponent.Factory,
    ): ExpressTransactionsComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(TokenDetailsModel::class)
    fun bindTokenDetailsModel(model: TokenDetailsModel): Model

    @Binds
    @IntoMap
    @ClassKey(ExpressTransactionsModel::class)
    fun bindExpressTransactionsModel(model: ExpressTransactionsModel): Model

    @Binds
    @Singleton
    fun bindExpressTransactionsEventListener(
        impl: DefaultExpressTransactionsEventListener,
    ): ExpressTransactionsEventListener
}

@Module
@InstallIn(ModelComponent::class)
internal interface StakingComponentModule {

    @Binds
    @ModelScoped
    fun bindRouter(impl: DefaultTokenDetailsRouter): InnerTokenDetailsRouter
}