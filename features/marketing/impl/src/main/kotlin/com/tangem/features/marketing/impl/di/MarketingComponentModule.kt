package com.tangem.features.marketing.impl.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.marketing.impl.DefaultMarketingBannerComponent
import com.tangem.features.marketing.impl.model.MarketingBannerModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface MarketingComponentModule {

    @Binds
    @Singleton
    fun bindMarketingBannerComponentFactory(
        factory: DefaultMarketingBannerComponent.Factory,
    ): MarketingBannerComponent.Factory
}

@Module
@InstallIn(ModelComponent::class)
internal interface MarketingModelModule {

    @Binds
    @IntoMap
    @ClassKey(MarketingBannerModel::class)
    fun bindMarketingBannerModel(model: MarketingBannerModel): Model
}