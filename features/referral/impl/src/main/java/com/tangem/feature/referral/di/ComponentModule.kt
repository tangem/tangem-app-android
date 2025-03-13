package com.tangem.feature.referral.di

import com.tangem.core.decompose.model.Model
import com.tangem.feature.referral.DefaultReferralComponent
import com.tangem.feature.referral.api.ReferralComponent
import com.tangem.feature.referral.model.ReferralModel
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
internal interface ComponentModule {

    @Binds
    fun provideReferralComponentFactory(impl: DefaultReferralComponent.Factory): ReferralComponent.Factory

    @Binds
    @IntoMap
    @ClassKey(ReferralModel::class)
    fun bindModel(model: ReferralModel): Model
}