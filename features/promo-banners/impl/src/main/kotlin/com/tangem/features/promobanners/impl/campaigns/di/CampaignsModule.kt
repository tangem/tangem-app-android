package com.tangem.features.promobanners.impl.campaigns.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.promobanners.api.deeplink.CampaignsDeepLinkHandler
import com.tangem.features.promobanners.api.swapcashback.CampaignsComponent
import com.tangem.features.promobanners.impl.campaigns.component.DefaultCampaignsComponent
import com.tangem.features.promobanners.impl.campaigns.deeplink.DefaultCampaignsDeepLinkHandler
import com.tangem.features.promobanners.impl.campaigns.model.ActivateCampaignsModel
import com.tangem.features.promobanners.impl.campaigns.model.CampaignsModel
import com.tangem.features.promobanners.impl.campaigns.service.CampaignsService
import com.tangem.features.promobanners.impl.campaigns.service.DefaultCampaignsService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CampaignsModule {

    @Binds
    @Singleton
    fun bindCampaignsComponentFactory(factory: DefaultCampaignsComponent.Factory): CampaignsComponent.Factory

    @Binds
    @Singleton
    fun bindCampaignsDeepLinkHandlerFactory(
        factory: DefaultCampaignsDeepLinkHandler.Factory,
    ): CampaignsDeepLinkHandler.Factory

    @Binds
    @Singleton
    fun bindCampaignsService(service: DefaultCampaignsService): CampaignsService
}

@Module
@InstallIn(ModelComponent::class)
internal interface CampaignsModelModule {

    @Binds
    @IntoMap
    @ClassKey(CampaignsModel::class)
    fun bindCampaignModel(model: CampaignsModel): Model

    @Binds
    @IntoMap
    @ClassKey(ActivateCampaignsModel::class)
    fun bindCampaignActivateModel(model: ActivateCampaignsModel): Model
}