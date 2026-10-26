package com.tangem.features.tangempay.di

import com.tangem.core.decompose.di.ModelComponent
import com.tangem.core.decompose.model.Model
import com.tangem.features.tangempay.campaign.TangemPayCampaignModel
import com.tangem.features.tangempay.hotwallet.TangemPayHotWalletOnboardingModel
import com.tangem.features.tangempay.model.TangemPayOnboardingModel
import com.tangem.features.tangempay.success.TangemPayOnboardingSuccessModel
import com.tangem.features.tangempay.verified.TangemPayVerifiedModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(ModelComponent::class)
internal interface TangemPayOnboardingModelsModule {

    @Binds
    @IntoMap
    @ClassKey(TangemPayOnboardingModel::class)
    fun bindTangemPayOnboardingModel(model: TangemPayOnboardingModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayHotWalletOnboardingModel::class)
    fun bindHotWalletOnboardingModel(model: TangemPayHotWalletOnboardingModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayCampaignModel::class)
    fun bindTangemPayCampaignModel(model: TangemPayCampaignModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayVerifiedModel::class)
    fun bindTangemPayVerifiedModel(model: TangemPayVerifiedModel): Model

    @Binds
    @IntoMap
    @ClassKey(TangemPayOnboardingSuccessModel::class)
    fun bindTangemPayOnboardingSuccessModel(model: TangemPayOnboardingSuccessModel): Model
}